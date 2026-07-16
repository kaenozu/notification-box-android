package com.notificationbox.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.data.settings.SettingsRepository
import com.notificationbox.app.domain.dryrun.DryRunPlanner
import com.notificationbox.app.domain.dryrun.DryRunPreview
import com.notificationbox.app.domain.dryrun.OrganizationMode
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.AppState
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationIngestionHealth
import com.notificationbox.app.model.NotificationItem
import com.notificationbox.app.permission.PermissionStatusProvider
import com.notificationbox.app.service.NotificationIngestionHealthStore
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationBoxViewModel(
    private val permissionProvider: PermissionStatusProvider,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationContentPresenter: NotificationContentPresenter =
        NotificationContentPresenter.Identity,
    private val clock: Clock = Clock.systemUTC(),
    private val dryRunPlanner: DryRunPlanner = DryRunPlanner(),
    ingestionHealth: StateFlow<NotificationIngestionHealth> =
        NotificationIngestionHealthStore.health
) : ViewModel() {

    data class DryRunState(
        val mode: OrganizationMode,
        val preview: DryRunPreview
    )

    private val notificationAccessGranted = MutableStateFlow(
        permissionProvider.isNotificationListenerGranted()
    )
    private val organizationMode = MutableStateFlow(OrganizationMode.OBSERVE_ONLY)
    private val mutableOperationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = mutableOperationMessage.asStateFlow()

    private val notifications: StateFlow<List<NotificationItem>> =
        notificationRepository.observeNotifications()
            .map { items -> items.map(notificationContentPresenter::present) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList()
            )

    private val storedState = combine(
        settingsRepository.settings,
        ingestionHealth
    ) { settings, health ->
        AppState(
            mode = settings.mode,
            preferencesLoaded = settings.preferencesLoaded,
            onboardingCompleted = settings.onboardingCompleted,
            digestSchedule = settings.digestSchedule,
            pausedUntilText = settings.pausedUntilText,
            selectedFilter = settings.selectedFilter,
            ingestionHealth = health
        )
    }

    val state: StateFlow<AppState> = combine(
        storedState,
        notifications,
        notificationRepository.observeAppRules(),
        notificationRepository.observeClassificationStats(),
        notificationAccessGranted
    ) { storeState, notificationItems, appRules, stats, listenerGranted ->
        storeState.copy(
            notificationAccessGranted = listenerGranted,
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
        notificationAccessGranted.value = permissionProvider.isNotificationListenerGranted()
    }

    fun completeOnboarding() = launchOperation("初回設定の保存に失敗しました") {
        settingsRepository.setOnboardingCompleted(true)
    }

    fun resetOnboarding() = launchOperation("初回説明の再表示設定に失敗しました") {
        settingsRepository.setOnboardingCompleted(false)
    }

    fun setMode(mode: AppMode) = launchOperation("モード設定の保存に失敗しました") {
        settingsRepository.setMode(mode)
    }

    fun setOrganizationMode(mode: OrganizationMode) {
        organizationMode.value = mode
    }

    fun setFilter(filter: NotificationDecision?) {
        settingsRepository.setFilter(filter)
    }

    fun pause(label: String) = launchOperation("一時停止設定の保存に失敗しました") {
        settingsRepository.pauseSummary(label)
    }

    fun setDigestHours(hours: List<Int>) = launchOperation("時刻設定の保存に失敗しました") {
        settingsRepository.setDigestHours(hours)
    }

    fun clearAll() = launchOperation("通知履歴を削除できませんでした") {
        notificationRepository.clearAll()
    }

    fun resetClassificationStats() = launchOperation("分類統計をリセットできませんでした") {
        notificationRepository.resetClassificationStats()
    }

    fun togglePinned(key: String, pinned: Boolean) = launchOperation("ピン留めを更新できませんでした") {
        notificationRepository.setPinned(key, pinned)
    }

    fun setNotificationDecision(key: String, decision: NotificationDecision?) =
        launchOperation("この通知の分類を更新できませんでした") {
            notificationRepository.setNotificationDecision(key, decision)
        }

    fun setAppRule(
        packageName: String,
        appLabel: String,
        decision: NotificationDecision?
    ) = launchOperation("アプリ別ルールを更新できませんでした") {
        notificationRepository.setAppRule(packageName, appLabel, decision)
    }

    fun delete(key: String) = launchOperation("通知履歴を削除できませんでした") {
        notificationRepository.delete(key)
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
        launchOperation("デモ通知を追加できませんでした") {
            records.forEach { notificationRepository.upsert(it) }
        }
    }

    fun consumeOperationMessage() {
        mutableOperationMessage.value = null
    }

    private fun launchOperation(
        failureMessage: String,
        operation: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                operation()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableOperationMessage.value = failureMessage
            }
        }
    }
}

class NotificationBoxViewModelFactory(
    private val permissionProvider: PermissionStatusProvider,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationContentPresenter: NotificationContentPresenter =
        NotificationContentPresenter.Identity
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationBoxViewModel::class.java)) {
            return NotificationBoxViewModel(
                permissionProvider = permissionProvider,
                notificationRepository = notificationRepository,
                settingsRepository = settingsRepository,
                notificationContentPresenter = notificationContentPresenter
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
