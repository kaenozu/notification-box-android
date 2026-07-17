package com.notificationbox.app.data.repository

import com.notificationbox.app.model.AppRule
import com.notificationbox.app.model.ClassificationStats
import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNotificationRepository : NotificationRepository {
    private val items = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val rules = MutableStateFlow<List<AppRule>>(emptyList())
    private val stats = MutableStateFlow(ClassificationStats())

    val pinnedUpdates = mutableListOf<Pair<String, Boolean>>()
    val decisionUpdates = mutableListOf<Pair<String, NotificationDecision?>>()
    val appRuleUpdates = mutableListOf<Triple<String, String, NotificationDecision?>>()
    val deletedKeys = mutableListOf<String>()
    var clearAllCalls: Int = 0
        private set
    var pruneCalls: Int = 0
        private set
    var resetStatsCalls: Int = 0
        private set

    override fun observeNotifications(): Flow<List<NotificationItem>> = items

    override fun observeAppRules(): Flow<List<AppRule>> = rules

    override fun observeClassificationStats(): Flow<ClassificationStats> = stats

    override suspend fun upsert(notification: NotificationRecord) {
        items.value = upsertInto(items.value, notification)
        stats.value = stats.value.copy(
            automaticallyClassified = stats.value.automaticallyClassified + 1,
            automaticByDecision = stats.value.automaticByDecision +
                (
                    notification.category to
                        ((stats.value.automaticByDecision[notification.category] ?: 0) + 1)
                    )
        )
    }

    override suspend fun synchronizeActive(
        activeKeys: Set<String>,
        notifications: List<NotificationRecord>,
        synchronizedAtMillis: Long
    ) {
        var synchronizedItems = items.value.map { item ->
            if (item.isActive && item.key !in activeKeys) {
                item.copy(
                    isActive = false,
                    removedAt = Instant.ofEpochMilli(synchronizedAtMillis)
                )
            } else {
                item
            }
        }
        notifications.forEach { notification ->
            synchronizedItems = upsertInto(synchronizedItems, notification)
        }
        items.value = synchronizedItems
    }

    override suspend fun markRemoved(key: String, removedAtMillis: Long) {
        items.value = items.value.map {
            if (it.key == key) {
                it.copy(
                    isActive = false,
                    removedAt = Instant.ofEpochMilli(removedAtMillis)
                )
            } else {
                it
            }
        }
    }

    override suspend fun setPinned(key: String, pinned: Boolean) {
        pinnedUpdates += key to pinned
        items.value = items.value.map {
            if (it.key == key) it.copy(userPinned = pinned) else it
        }
    }

    override suspend fun setNotificationDecision(
        key: String,
        decision: NotificationDecision?
    ) {
        decisionUpdates += key to decision
        items.value = items.value.map { item ->
            if (item.key != key) return@map item
            val finalDecision = decision ?: item.appRuleDecision ?: item.automaticDecision
            val source = when {
                decision != null -> DecisionSource.UserOverride
                item.appRuleDecision != null -> DecisionSource.AppRule
                else -> DecisionSource.Automatic
            }
            item.copy(
                userDecision = decision,
                category = finalDecision,
                decisionSource = source,
                reason = sourceReason(item, finalDecision, source)
            )
        }
        stats.value = stats.value.copy(
            userOverrideChanges = stats.value.userOverrideChanges + 1
        )
    }

    override suspend fun setAppRule(
        packageName: String,
        appLabel: String,
        decision: NotificationDecision?
    ) {
        appRuleUpdates += Triple(packageName, appLabel, decision)
        rules.value = if (decision == null) {
            rules.value.filterNot { it.packageName == packageName }
        } else {
            listOf(AppRule(packageName, appLabel, decision, Instant.EPOCH)) +
                rules.value.filterNot { it.packageName == packageName }
        }
        items.value = items.value.map { item ->
            if (item.packageName != packageName) return@map item
            val finalDecision = item.userDecision ?: decision ?: item.automaticDecision
            val source = when {
                item.userDecision != null -> DecisionSource.UserOverride
                decision != null -> DecisionSource.AppRule
                else -> DecisionSource.Automatic
            }
            item.copy(
                appRuleDecision = decision,
                category = finalDecision,
                decisionSource = source,
                reason = sourceReason(item, finalDecision, source)
            )
        }
        stats.value = stats.value.copy(
            appRuleChanges = stats.value.appRuleChanges + 1
        )
    }

    override suspend fun delete(key: String) {
        deletedKeys += key
        items.value = items.value.filterNot { it.key == key }
    }

    override suspend fun clearAll() {
        clearAllCalls++
        items.value = emptyList()
    }

    override suspend fun pruneExpired() {
        pruneCalls++
    }

    override suspend fun resetClassificationStats() {
        resetStatsCalls++
        stats.value = ClassificationStats()
    }

    fun emit(newItems: List<NotificationItem>) {
        items.value = newItems
    }

    fun emitRules(newRules: List<AppRule>) {
        rules.value = newRules
    }

    fun emitStats(newStats: ClassificationStats) {
        stats.value = newStats
    }

    private fun upsertInto(
        currentItems: List<NotificationItem>,
        notification: NotificationRecord
    ): List<NotificationItem> {
        val existing = currentItems.firstOrNull { it.key == notification.key }
        val appRuleDecision = rules.value.firstOrNull {
            it.packageName == notification.packageName
        }?.decision
        val userDecision = existing?.userDecision
        val finalDecision = userDecision ?: appRuleDecision ?: notification.category
        val source = when {
            userDecision != null -> DecisionSource.UserOverride
            appRuleDecision != null -> DecisionSource.AppRule
            else -> DecisionSource.Automatic
        }
        val mapped = NotificationItem(
            key = notification.key,
            packageName = notification.packageName,
            appLabel = notification.appLabel,
            title = notification.title,
            text = notification.text,
            postTime = Instant.ofEpochMilli(notification.postTimeMillis),
            automaticDecision = notification.category,
            userDecision = userDecision,
            appRuleDecision = appRuleDecision,
            category = finalDecision,
            decisionSource = source,
            automaticReason = notification.reason,
            reason = sourceReason(
                notification.appLabel,
                notification.reason,
                finalDecision,
                source
            ),
            contentAvailability = notification.contentAvailability,
            userPinned = existing?.userPinned ?: false,
            isActive = notification.isActive,
            removedAt = notification.removedAtMillis?.let(Instant::ofEpochMilli)
        )
        return listOf(mapped) + currentItems.filterNot { it.key == mapped.key }
    }

    private fun sourceReason(
        item: NotificationItem,
        decision: NotificationDecision,
        source: DecisionSource
    ): String = sourceReason(
        item.appLabel,
        item.automaticReason,
        decision,
        source
    )

    private fun sourceReason(
        appLabel: String,
        automaticReason: String,
        decision: NotificationDecision,
        source: DecisionSource
    ): String = when (source) {
        DecisionSource.Automatic -> automaticReason
        DecisionSource.AppRule -> "${appLabel}を「${decision.name}」に設定済み"
        DecisionSource.UserOverride ->
            "ユーザーがこの通知を「${decision.name}」に変更"
    }
}
