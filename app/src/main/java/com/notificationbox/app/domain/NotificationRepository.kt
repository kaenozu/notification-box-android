package com.notificationbox.app.domain

import com.notificationbox.app.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<List<NotificationRecord>>
    suspend fun upsert(record: NotificationRecord)
    suspend fun markRemoved(key: String, removedAtMillis: Long)
    suspend fun setPinned(key: String, pinned: Boolean)
    suspend fun delete(key: String)
    suspend fun clearAll()
    suspend fun prune()
}
