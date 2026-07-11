package com.notificationbox.app.data

import com.notificationbox.app.domain.toRecord
import com.notificationbox.app.domain.NotificationRecord
import com.notificationbox.app.domain.NotificationRepository
import com.notificationbox.app.domain.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val MAX_NOTIFICATIONS = 500
private const val RETENTION_DAYS = 7L
private const val MILLIS_PER_DAY = 86_400_000L

class RoomNotificationRepository(
    private val db: NotificationDatabase
) : NotificationRepository {

    private val dao = db.dao()

    override fun observeNotifications(): Flow<List<NotificationRecord>> {
        return dao.observeAll().map { list ->
            list.map { it.toRecord() }
        }
    }

    override suspend fun upsert(record: NotificationRecord) {
        dao.upsert(record.toEntity())
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
        dao.deleteAll()
    }

    override suspend fun prune() {
        val cutoff = System.currentTimeMillis() - RETENTION_DAYS * MILLIS_PER_DAY
        dao.deleteOldUnpinned(cutoff)

        val unpinnedCount = dao.countUnpinned()
        if (unpinnedCount > MAX_NOTIFICATIONS) {
            val excess = unpinnedCount - MAX_NOTIFICATIONS
            dao.deleteOldestUnpinned(excess)
        }
    }
}
