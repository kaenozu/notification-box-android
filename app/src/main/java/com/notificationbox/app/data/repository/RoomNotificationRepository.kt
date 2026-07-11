package com.notificationbox.app.data.repository

import com.notificationbox.app.data.db.NotificationDao
import com.notificationbox.app.data.db.NotificationEntity
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNotificationRepository(
    private val dao: NotificationDao,
    private val clock: Clock = Clock.systemUTC()
) : NotificationRepository {

    override fun observeNotifications(): Flow<List<NotificationItem>> =
        dao.observeAll().map { entities -> entities.map(NotificationEntity::toModel) }

    override suspend fun upsert(notification: NotificationRecord) {
        val existing = dao.getByKey(notification.key)
        dao.upsert(notification.toEntity(userPinned = existing?.userPinned ?: false))
        prune()
    }

    override suspend fun markRemoved(key: String, removedAtMillis: Long) {
        dao.markRemoved(key, removedAtMillis)
    }

    override suspend fun setPinned(key: String, pinned: Boolean) {
        dao.setPinned(key, pinned)
    }

    override suspend fun delete(key: String) {
        dao.deleteByKey(key)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    private suspend fun prune() {
        val cutoff = clock.millis() - RETENTION.toMillis()
        dao.deleteExpired(cutoff)
        dao.pruneToMaximum(MAX_NOTIFICATION_COUNT)
    }

    private fun NotificationRecord.toEntity(userPinned: Boolean): NotificationEntity =
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
            userPinned = userPinned,
            isActive = isActive,
            removedAtMillis = removedAtMillis
        )

    private fun NotificationEntity.toModel(): NotificationItem =
        NotificationItem(
            key = key,
            packageName = packageName,
            appLabel = appLabel,
            title = title,
            text = text,
            postTime = Instant.ofEpochMilli(postTimeMillis),
            category = category.toDecisionOrDefault(),
            reason = reason,
            userPinned = userPinned,
            isActive = isActive,
            removedAt = removedAtMillis?.let(Instant::ofEpochMilli)
        )

    private fun String.toDecisionOrDefault(): NotificationDecision =
        runCatching { NotificationDecision.valueOf(this) }
            .getOrDefault(NotificationDecision.HoldForDigest)

    companion object {
        internal const val MAX_NOTIFICATION_COUNT = 500
        internal val RETENTION: Duration = Duration.ofDays(7)
    }
}
