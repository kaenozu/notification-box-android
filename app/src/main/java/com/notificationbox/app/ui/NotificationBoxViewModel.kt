package com.notificationbox.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationbox.app.data.NotificationStore
import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.domain.dryrun.DryRunPlanner
import com.notificationbox.app.domain.dryrun.DryRunPreview
import com.notificationbox.app.domain.dryrun.OrganizationMode
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.AppState
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import com.notificationbox.app.permission.PermissionStatusProvider
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationBoxViewModel(
    private val permissionProvider: PermissionStatusProvider,
    private val notificationRepository: NotificationRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val dryRunPlanner: DryRunPlanner = DryRunPlanner()
) : ViewModel() {

    data class PermissionState(
        val notificationAccessGranted: Boolean,
        val postNotificationsGranted: Boolean
    )

    data class DryRunState(
        val mode: OrganizationMode,
        val preview: DryRunPreview
    )

    private val permissionState = MutableStateFlow(readPermissionState())
    private val organizationMode = MutableStateFlow(OrganizationMode.OBSERVE_ONLY)

    // Share one repository snapshot so permission, rule-list, and statistics changes do not
    // cause duplicate repository collection or unnecessary dry-run planning.
    private val notifications: StateFlow<List<NotificationItem>> =
        notificationRepository.observeNotifications().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    val state: StateFlow<AppState> = combine(
        NotificationStore.state,
        notifications,
        notificationRepository.observeAppRules(),
        notificationRepository.observeClassificationStats(),
        permissionState
    ) { storeState, notificationItems, appRules, stats, permissions ->
        storeState.copy(
            notificationAccessGranted = permissions.notificationAccessGranted,
            postNotificationsGranted = permissions.postNotificationsGranted,
            items = notificationItems,
            appRules = appRules,
            classificationStats = stats
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    val dryRunState: StateFlow<DryRunState> = combine(
        organizationMode,
        notifications
    ) { mode, notificationItems ->
        DryRunState(
            mode = mode,
            preview = dryRunPlanner.plan(mode, notificationItems)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        DryRunState(
            mode = OrganizationMode.OBSERVE_ONLY,
            preview = DryRunPreview.observeOnly(activeNotificationCount = 0)
        )
    )

    fun refreshPermissions() {
        permissionState.value = readPermissionState()
    }

    fun setMode(mode: AppMode) = NotificationStore.setMode(mode)

    fun setOrganizationMode(mode: OrganizationMode) {
        organizationMode.value = mode
    }

    fun setFilter(filter: NotificationDecision?) = NotificationStore.setFilter(filter)

    fun pause(label: String) = NotificationStore.pauseSummary(label)

    fun setDigestHours(hours: List<Int>) = NotificationStore.setDigestHours(hours)

    fun clearAll() {
        viewModelScope.launch { notificationRepository.clearAll() }
    }

    fun togglePinned(key: String, pinned: Boolean) {
        viewModelScope.launch { notificationRepository.setPinned(key, pinned) }
    }

    fun setNotificationDecision(key: String, decision: NotificationDecision?) {
        viewModelScope.launch {
            notificationRepository.setNotificationDecision(key, decision)
        }
    }

    fun setAppRule(
        packageName: String,
        appLabel: String,
        decision: NotificationDecision?
    ) {
        viewModelScope.launch {
            notificationRepository.setAppRule(packageName, appLabel, decision)
        }
    }

    fun delete(key: String) {
        viewModelScope.launch { notificationRepository.delete(key) }
    }

    fun seed() {
        val now = clock.millis()
        val records = listOf(
            NotificationRecord(
                key = "demo:${UUID.randomUUID()}",
                packageName = "com.google.android.gm",
                appLabel = "Gmail",
                title = "会議の返信",
                text = "今日の15時で大丈夫ですか",
                postTimeMillis = now,
                notificationId = 1,
                tag = null,
                channelId = "demo",
                category = NotificationDecision.HoldForDigest,
                reason = "デバッグ用通知"
            ),
            NotificationRecord(
                key = "demo:${UUID.randomUUID()}",
                packageName = "com.bank.app",
                appLabel = "Bank",
                title = "認証コード",
                text = "確認コード 482913",
                postTimeMillis = now - 1,
                notificationId = 2,
                tag = null,
                channelId = "demo",
                category = NotificationDecision.KeepNow,
                reason = "デバッグ用通知"
            )
        )
        viewModelScope.launch {
            records.forEach { notificationRepository.upsert(it) }
        }
    }

    private fun readPermissionState(): PermissionState =
        PermissionState(
            notificationAccessGranted = permissionProvider.isNotificationListenerGranted(),
            postNotificationsGranted = permissionProvider.canPostNotifications()
        )
}

class NotificationBoxViewModelFactory(
    private val permissionProvider: PermissionStatusProvider,
    private val notificationRepository: NotificationRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationBoxViewModel::class.java)) {
            return NotificationBoxViewModel(permissionProvider, notificationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
