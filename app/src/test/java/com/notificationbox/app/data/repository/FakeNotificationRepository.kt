package com.notificationbox.app.data.repository

import com.notificationbox.app.model.NotificationItem
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNotificationRepository : NotificationRepository {
    private val items = MutableStateFlow<List<NotificationItem>>(emptyList())

    val pinnedUpdates = mutableListOf<Pair<String, Boolean>>()
    val deletedKeys = mutableListOf<String>()
    var clearAllCalls: Int = 0
        private set

    override fun observeNotifications(): Flow<List<NotificationItem>> = items

    override suspend fun upsert(notification: NotificationRecord) {
        val existing = items.value.firstOrNull { it.key == notification.key }
        val mapped = NotificationItem(
            key = notification.key,
            packageName = notification.packageName,
            appLabel = notification.appLabel,
            title = notification.title,
            text = notification.text,
            postTime = Instant.ofEpochMilli(notification.postTimeMillis),
            category = notification.category,
            reason = notification.reason,
            userPinned = existing?.userPinned ?: false,
            isActive = notification.isActive,
            removedAt = notification.removedAtMillis?.let(Instant::ofEpochMilli)
        )
        items.value = listOf(mapped) + items.value.filterNot { it.key == mapped.key }
    }

    override suspend fun markRemoved(key: String, removedAtMillis: Long) {
        items.value = items.value.map {
            if (it.key == key) it.copy(isActive = false, removedAt = Instant.ofEpochMilli(removedAtMillis)) else it
        }
    }

    override suspend fun setPinned(key: String, pinned: Boolean) {
        pinnedUpdates += key to pinned
        items.value = items.value.map { if (it.key == key) it.copy(userPinned = pinned) else it }
    }

    override suspend fun delete(key: String) {
        deletedKeys += key
        items.value = items.value.filterNot { it.key == key }
    }

    override suspend fun clearAll() {
        clearAllCalls++
        items.value = emptyList()
    }

    fun emit(newItems: List<NotificationItem>) {
        items.value = newItems
    }
}
