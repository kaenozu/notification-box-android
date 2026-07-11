package com.notificationbox.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationbox.app.data.NotificationStore
import com.notificationbox.app.domain.NotificationSample
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.AppState
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.permission.PermissionStatusProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class NotificationBoxViewModel(
    private val permissionProvider: PermissionStatusProvider
) : ViewModel() {

    private val permissionRefreshTrigger = MutableStateFlow(0)

    val state: StateFlow<AppState> = combine(
        NotificationStore.state,
        permissionRefreshTrigger
    ) { storeState, _ ->
        storeState.copy(
            notificationAccessGranted = permissionProvider.isNotificationListenerGranted(),
            postNotificationsGranted = permissionProvider.canPostNotifications()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppState())

    fun refreshPermissions() {
        permissionRefreshTrigger.value = permissionRefreshTrigger.value + 1
    }

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

class NotificationBoxViewModelFactory(
    private val permissionProvider: PermissionStatusProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationBoxViewModel::class.java)) {
            return NotificationBoxViewModel(permissionProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
