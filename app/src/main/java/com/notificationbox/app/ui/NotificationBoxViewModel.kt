package com.notificationbox.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationbox.app.domain.NotificationClassifier
import com.notificationbox.app.domain.NotificationRecord
import com.notificationbox.app.domain.NotificationRepository
import com.notificationbox.app.domain.NotificationSample
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.AppState
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.permission.PermissionStatusProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationBoxViewModel(
    private val permissionProvider: PermissionStatusProvider,
    private val repository: NotificationRepository
) : ViewModel() {

    data class PermissionState(
        val notificationAccessGranted: Boolean,
        val postNotificationsGranted: Boolean
    )

    private val _permissionState = MutableStateFlow(
        PermissionState(
            notificationAccessGranted = permissionProvider.isNotificationListenerGranted(),
            postNotificationsGranted = permissionProvider.canPostNotifications()
        )
    )

    private val repositoryItems = repository.observeNotifications()
        .map { list ->
            list.map { record ->
                com.notificationbox.app.model.NotificationItem(
                    notificationKey = record.key,
                    packageName = record.packageName,
                    appLabel = record.appLabel,
                    title = record.title,
                    text = record.text,
                    postTime = java.time.Instant.ofEpochMilli(record.postTimeMillis),
                    category = record.category,
                    reason = record.reason,
                    userPinned = record.userPinned,
                    isActive = record.isActive,
                    removedAtMillis = record.removedAtMillis
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val permissionState: StateFlow<PermissionState> = _permissionState

    private val storeState = com.notificationbox.app.data.NotificationStore.state

    val state: StateFlow<AppState> = combine(
        storeState,
        _permissionState,
        repositoryItems
    ) { store, permissions, items ->
        store.copy(
            notificationAccessGranted = permissions.notificationAccessGranted,
            postNotificationsGranted = permissions.postNotificationsGranted,
            items = items
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, AppState())

    fun refreshPermissions() {
        _permissionState.value = PermissionState(
            notificationAccessGranted = permissionProvider.isNotificationListenerGranted(),
            postNotificationsGranted = permissionProvider.canPostNotifications()
        )
    }

    private val classifier = NotificationClassifier()

    fun ingestFromListener(
        key: String, packageName: String, appLabel: String,
        title: String?, text: String?,
        postTimeMillis: Long, notificationId: Int, tag: String?, channelId: String?
    ) {
        val sample = NotificationSample(packageName, title, text)
        val (decision, reason) = classifier.classify(sample)
        val record = NotificationRecord(
            key = key, packageName = packageName, appLabel = appLabel,
            title = title, text = text, postTimeMillis = postTimeMillis,
            notificationId = notificationId, tag = tag, channelId = channelId,
            category = decision, reason = reason,
            userPinned = false, isActive = true, removedAtMillis = null
        )
        viewModelScope.launch {
            repository.upsert(record)
        }
    }

    fun markRemoved(key: String) {
        viewModelScope.launch {
            repository.markRemoved(key, System.currentTimeMillis())
        }
    }

    fun setMode(mode: AppMode) = com.notificationbox.app.data.NotificationStore.setMode(mode)

    fun setFilter(filter: NotificationDecision?) = com.notificationbox.app.data.NotificationStore.setFilter(filter)

    fun pause(label: String) = com.notificationbox.app.data.NotificationStore.pauseSummary(label)

    fun setDigestHours(hours: List<Int>) = com.notificationbox.app.data.NotificationStore.setDigestHours(hours)

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }

    fun seed() {
        viewModelScope.launch {
            listOf(
                Triple("com.google.android.gm", "会議の返信", "今日の15時で大丈夫ですか"),
                Triple("com.bank.app", "認証コード", "確認コード 482913"),
                Triple("com.shop.app", "今だけ50%OFF", "キャンペーン終了間近"),
            ).forEach { (pkg, title, text) ->
                val sample = NotificationSample(pkg, title, text)
                val (decision, reason) = classifier.classify(sample)
                val key = "demo-$pkg-${System.currentTimeMillis()}"
                val record = NotificationRecord(
                    key = key, packageName = pkg, appLabel = pkg.substringAfterLast('.'),
                    title = title, text = text, postTimeMillis = System.currentTimeMillis(),
                    notificationId = 0, tag = null, channelId = null,
                    category = decision, reason = reason,
                    userPinned = false, isActive = true, removedAtMillis = null
                )
                repository.upsert(record)
            }
        }
    }

    fun togglePinned(key: String, pinned: Boolean) {
        viewModelScope.launch { repository.setPinned(key, pinned) }
    }

    fun delete(key: String) {
        viewModelScope.launch { repository.delete(key) }
    }
}

class NotificationBoxViewModelFactory(
    private val permissionProvider: PermissionStatusProvider,
    private val repository: NotificationRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationBoxViewModel::class.java)) {
            return NotificationBoxViewModel(permissionProvider, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
