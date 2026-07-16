package com.notificationbox.app.data.repository

import androidx.room.withTransaction
import com.notificationbox.app.data.db.AppRuleEntity
import com.notificationbox.app.data.db.ClassificationStatEntity
import com.notificationbox.app.data.db.NotificationDatabase
import com.notificationbox.app.data.db.NotificationEntity
import com.notificationbox.app.model.AppRule
import com.notificationbox.app.model.ClassificationStats
import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.NotificationContentAvailability
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomNotificationRepository(
    private val database: NotificationDatabase,
    private val clock: Clock = Clock.systemUTC()
) : NotificationRepository {
    private val notificationDao = database.notificationDao()
    private val appRuleDao = database.appRuleDao()
    private val classificationStatsDao = database.classificationStatsDao()
    private val mutationMutex = Mutex()

    override fun observeNotifications(): Flow<List<NotificationItem>> =
        combine(
            notificationDao.observeAll(),
            appRuleDao.observeAll()
        ) { entities, rules ->
            val rulesByPackage = rules.associateBy(AppRuleEntity::packageName)
            entities.map { entity -> entity.toModel(rulesByPackage[entity.packageName]) }
        }.flowOn(Dispatchers.Default)

    override fun observeAppRules(): Flow<List<AppRule>> =
        appRuleDao.observeAll().map { rules -> rules.map { it.toModel() } }

    override fun observeClassificationStats(): Flow<ClassificationStats> =
        classificationStatsDao.observeAll().map(::toClassificationStats)

    override suspend fun upsert(notification: NotificationRecord) {
        mutationMutex.withLock {
            database.withTransaction {
                upsertLocked(notification)
                pruneLocked()
            }
        }
    }

    override suspend fun synchronizeActive(
        activeKeys: Set<String>,
        notifications: List<NotificationRecord>,
        synchronizedAtMillis: Long
    ) {
        mutationMutex.withLock {
            database.withTransaction {
                if (activeKeys.isEmpty()) {
                    notificationDao.markAllActiveRemoved(synchronizedAtMillis)
                } else {
                    notificationDao.markActiveMissing(activeKeys.toList(), synchronizedAtMillis)
                }
                notifications.forEach { upsertLocked(it) }
                pruneLocked()
            }
        }
    }

    override suspend fun markRemoved(key: String, removedAtMillis: Long) {
        mutationMutex.withLock {
            database.withTransaction {
                notificationDao.markRemoved(key, removedAtMillis)
                pruneLocked()
            }
        }
    }

    override suspend fun setPinned(key: String, pinned: Boolean) {
        mutationMutex.withLock {
            database.withTransaction {
                notificationDao.setPinned(key, pinned)
                if (!pinned) {
                    pruneLocked()
                }
            }
        }
    }

    override suspend fun setNotificationDecision(
        key: String,
        decision: NotificationDecision?
    ) {
        mutationMutex.withLock {
            database.withTransaction {
                val existing = notificationDao.getByKey(key) ?: return@withTransaction
                val serialized = decision?.name
                if (existing.userDecision == serialized) return@withTransaction

                notificationDao.setUserDecision(key, serialized)
                classificationStatsDao.increment(STAT_USER_OVERRIDE_CHANGES)
                classificationStatsDao.increment(appChangeKey(existing.packageName))
                decision?.let {
                    classificationStatsDao.increment(selectedDecisionKey(it))
                }
            }
        }
    }

    override suspend fun setAppRule(
        packageName: String,
        appLabel: String,
        decision: NotificationDecision?
    ) {
        mutationMutex.withLock {
            database.withTransaction {
                val existing = appRuleDao.getByPackageName(packageName)
                if (decision == null) {
                    if (existing == null) return@withTransaction
                    appRuleDao.delete(packageName)
                } else {
                    if (existing?.decision == decision.name && existing.appLabel == appLabel) {
                        return@withTransaction
                    }
                    appRuleDao.upsert(
                        AppRuleEntity(
                            packageName = packageName,
                            appLabel = appLabel,
                            decision = decision.name,
                            updatedAtMillis = clock.millis()
                        )
                    )
                }

                classificationStatsDao.increment(STAT_APP_RULE_CHANGES)
                classificationStatsDao.increment(appChangeKey(packageName))
                decision?.let {
                    classificationStatsDao.increment(selectedDecisionKey(it))
                }
            }
        }
    }

    override suspend fun delete(key: String) {
        mutationMutex.withLock {
            notificationDao.deleteByKey(key)
        }
    }

    override suspend fun clearAll() {
        mutationMutex.withLock {
            notificationDao.clearAll()
        }
    }

    override suspend fun pruneExpired() {
        mutationMutex.withLock {
            database.withTransaction {
                pruneLocked()
            }
        }
    }

    override suspend fun resetClassificationStats() {
        mutationMutex.withLock {
            classificationStatsDao.clearAll()
        }
    }

    private suspend fun upsertLocked(notification: NotificationRecord) {
        val existing = notificationDao.getByKey(notification.key)
        notificationDao.upsert(
            notification.toEntity(
                userDecision = existing?.userDecision,
                userPinned = existing?.userPinned ?: false
            )
        )
        if (existing == null) {
            classificationStatsDao.increment(STAT_AUTOMATIC_TOTAL)
            classificationStatsDao.increment(automaticDecisionKey(notification.category))
        }
    }

    private suspend fun pruneLocked() {
        val cutoff = clock.millis() - RETENTION.toMillis()
        notificationDao.deleteExpired(cutoff)
        notificationDao.pruneToMaximum(MAX_NOTIFICATION_COUNT)
    }

    private fun NotificationRecord.toEntity(
        userDecision: String?,
        userPinned: Boolean
    ): NotificationEntity =
        NotificationEntity(
            key = key,
            packageName = packageName,
            appLabel = appLabel,
            title = title,
            text = text,
            postTimeMillis = postTimeMillis,
            notificationId = notificationId,
            tag = tag,
            channelId = channelId,
            category = category.name,
            reason = reason,
            userDecision = userDecision,
            contentAvailability = contentAvailability.name,
            userPinned = userPinned,
            isActive = isActive,
            removedAtMillis = removedAtMillis
        )

    private fun NotificationEntity.toModel(appRule: AppRuleEntity?): NotificationItem {
        val automaticDecision = category.toDecisionOrDefault()
        val userOverride = userDecision.toDecisionOrNull()
        val ruleDecision = appRule?.decision.toDecisionOrNull()
        val finalDecision = userOverride ?: ruleDecision ?: automaticDecision
        val source = when {
            userOverride != null -> DecisionSource.UserOverride
            ruleDecision != null -> DecisionSource.AppRule
            else -> DecisionSource.Automatic
        }

        return NotificationItem(
            key = key,
            packageName = packageName,
            appLabel = appLabel,
            title = title,
            text = text,
            postTime = Instant.ofEpochMilli(postTimeMillis),
            automaticDecision = automaticDecision,
            userDecision = userOverride,
            appRuleDecision = ruleDecision,
            category = finalDecision,
            decisionSource = source,
            automaticReason = reason,
            reason = reason,
            contentAvailability = contentAvailability.toContentAvailability(),
            userPinned = userPinned,
            isActive = isActive,
            removedAt = removedAtMillis?.let(Instant::ofEpochMilli)
        )
    }

    private fun AppRuleEntity.toModel(): AppRule =
        AppRule(
            packageName = packageName,
            appLabel = appLabel,
            decision = decision.toDecisionOrDefault(),
            updatedAt = Instant.ofEpochMilli(updatedAtMillis)
        )

    private fun toClassificationStats(
        entities: List<ClassificationStatEntity>
    ): ClassificationStats {
        val counts = entities.associate { it.key to it.count }
        return ClassificationStats(
            automaticallyClassified = counts[STAT_AUTOMATIC_TOTAL] ?: 0,
            userOverrideChanges = counts[STAT_USER_OVERRIDE_CHANGES] ?: 0,
            appRuleChanges = counts[STAT_APP_RULE_CHANGES] ?: 0,
            automaticByDecision = NotificationDecision.entries.associateWith { decision ->
                counts[automaticDecisionKey(decision)] ?: 0
            },
            selectedByDecision = NotificationDecision.entries.associateWith { decision ->
                counts[selectedDecisionKey(decision)] ?: 0
            },
            appChangeCounts = counts
                .filterKeys { it.startsWith(STAT_APP_PREFIX) }
                .mapKeys { (key, _) -> key.removePrefix(STAT_APP_PREFIX) }
        )
    }

    private fun String?.toDecisionOrNull(): NotificationDecision? =
        this?.let { value ->
            runCatching { NotificationDecision.valueOf(value) }.getOrNull()
        }

    private fun String.toDecisionOrDefault(): NotificationDecision =
        toDecisionOrNull() ?: NotificationDecision.HoldForDigest

    private fun String.toContentAvailability(): NotificationContentAvailability =
        runCatching { NotificationContentAvailability.valueOf(this) }
            .getOrDefault(NotificationContentAvailability.REDACTED_OR_UNAVAILABLE)

    companion object {
        internal const val MAX_NOTIFICATION_COUNT = 500
        internal val RETENTION: Duration = Duration.ofDays(7)

        private const val STAT_AUTOMATIC_TOTAL = "automatic.total"
        private const val STAT_USER_OVERRIDE_CHANGES = "override.total"
        private const val STAT_APP_RULE_CHANGES = "rule.total"
        private const val STAT_AUTOMATIC_PREFIX = "automatic.decision."
        private const val STAT_SELECTED_PREFIX = "selected.decision."
        private const val STAT_APP_PREFIX = "app."

        private fun automaticDecisionKey(decision: NotificationDecision): String =
            STAT_AUTOMATIC_PREFIX + decision.name

        private fun selectedDecisionKey(decision: NotificationDecision): String =
            STAT_SELECTED_PREFIX + decision.name

        private fun appChangeKey(packageName: String): String =
            STAT_APP_PREFIX + packageName
    }
}
