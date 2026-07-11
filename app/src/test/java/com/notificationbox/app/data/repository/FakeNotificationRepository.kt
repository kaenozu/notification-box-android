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
        items.value = upsertInto(items.value, notification)
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

    private fun upsertInto(
        currentItems: List<NotificationItem>,
        notification: NotificationRecord
    ): List<NotificationItem> {
        val existing = currentItems.firstOrNull { it.key == notification.key }
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
        return listOf(mapped) + currentItems.filterNot { it.key == mapped.key }
    }
}
