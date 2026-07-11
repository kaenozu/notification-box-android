package com.notificationbox.app.data.repository

import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import kotlinx.coroutines.flow.Flow

data class NotificationRecord(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postTimeMillis: Long,
    val notificationId: Int,
    val tag: String?,
    val channelId: String?,
    val category: NotificationDecision,
    val reason: String,
    val isActive: Boolean = true,
    val removedAtMillis: Long? = null
)

interface NotificationRepository {
    fun observeNotifications(): Flow<List<NotificationItem>>

    suspend fun upsert(notification: NotificationRecord)

    suspend fun synchronizeActive(
        activeKeys: Set<String>,
        notifications: List<NotificationRecord>,
        synchronizedAtMillis: Long
    )

    suspend fun markRemoved(key: String, removedAtMillis: Long)

    suspend fun setPinned(key: String, pinned: Boolean)

    suspend fun delete(key: String)

    suspend fun clearAll()
}
