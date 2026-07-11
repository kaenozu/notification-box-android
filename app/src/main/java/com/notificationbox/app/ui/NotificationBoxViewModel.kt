package com.notificationbox.app.ui

import androidx.lifecycle.ViewModel
import com.notificationbox.app.data.NotificationStore
import com.notificationbox.app.domain.NotificationSample
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.NotificationDecision
import kotlinx.coroutines.flow.StateFlow

class NotificationBoxViewModel(
) : ViewModel() {
    val state: StateFlow<com.notificationbox.app.model.AppState> = NotificationStore.state

    fun enableNotificationAccess() = NotificationStore.grantNotificationAccess(true)
    fun enablePostNotifications() = NotificationStore.grantPostNotifications(true)
    fun setMode(mode: AppMode) = NotificationStore.setMode(mode)
    fun setFilter(filter: NotificationDecision?) = NotificationStore.setFilter(filter)
    fun pause(label: String) = NotificationStore.pauseSummary(label)
    fun setDigestHours(hours: List<Int>) = NotificationStore.setDigestHours(hours)
    fun clearAll() = NotificationStore.clearAll()
    fun seed() = NotificationStore.seedDemoTraffic()
    fun togglePinned(id: Long, pinned: Boolean) = NotificationStore.markPinned(id, pinned)
    fun delete(id: Long) = NotificationStore.deleteItem(id)
    fun ingestDemo(packageName: String, title: String, text: String) =
        NotificationStore.addNotification(NotificationSample(packageName, title, text))
}
