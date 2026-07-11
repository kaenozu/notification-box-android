package com.notificationbox.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNotificationRepository : NotificationRepository {

    private val records = MutableStateFlow<List<NotificationRecord>>(emptyList())

    override fun observeNotifications(): Flow<List<NotificationRecord>> = records

    var upsertCount = 0
    var markRemovedCount = 0
    var setPinnedCount = 0
    var deleteCount = 0
    var clearAllCount = 0

    override suspend fun upsert(record: NotificationRecord) {
        upsertCount++
        val current = records.value.toMutableList()
        val idx = current.indexOfFirst { it.key == record.key }
        if (idx >= 0) current[idx] = record else current.add(0, record)
        records.value = current
    }

    override suspend fun markRemoved(key: String, removedAtMillis: Long) {
        markRemovedCount++
        records.value = records.value.map {
            if (it.key == key) it.copy(isActive = false, removedAtMillis = removedAtMillis) else it
        }
    }

    override suspend fun setPinned(key: String, pinned: Boolean) {
        setPinnedCount++
        records.value = records.value.map {
            if (it.key == key) it.copy(userPinned = pinned) else it
        }
    }

    override suspend fun delete(key: String) {
        deleteCount++
        records.value = records.value.filterNot { it.key == key }
    }

    override suspend fun clearAll() {
        clearAllCount++
        records.value = emptyList()
    }

    override suspend fun prune() {}

    fun addRecord(record: NotificationRecord) {
        records.value = records.value + record
    }

    fun resetCounts() {
        upsertCount = 0
        markRemovedCount = 0
        setPinnedCount = 0
        deleteCount = 0
        clearAllCount = 0
    }
}
