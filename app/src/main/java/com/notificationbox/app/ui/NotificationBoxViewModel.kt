package com.notificationbox.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.data.settings.AppSettings
import com.notificationbox.app.data.settings.SettingsRepository
import com.notificationbox.app.domain.dryrun.DryRunPlanner
import com.notificationbox.app.domain.dryrun.DryRunPreview
import com.notificationbox.app.domain.dryrun.OrganizationMode
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.AppRule
import com.notificationbox.app.model.AppState
import com.notificationbox.app.model.ClassificationStats
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationIngestionHealth
import com.notificationbox.app.model.NotificationItem
import com.notificationbox.app.permission.PermissionStatusProvider
import com.notificationbox.app.service.NotificationIngestionHealthStore
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class NotificationHistoryUiState(
    val items: List<NotificationItem> = emptyList(),
    val selectedFilter: NotificationDecision? = null,
    val notificationAccessGranted: Boolean = false,
    val ingestionHealth: NotificationIngestionHealth = NotificationIngestionHealth(),
    val readFailed: Boolean = false
)

data class SettingsRulesUiState(
    val settings: AppSettings = AppSettings(),
    val appRules: List<AppRule> = emptyList(),
    val classificationStats: ClassificationStats = ClassificationStats(),
    val readFailed: Boolean = false
)

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

    private val notificationReadFailed = MutableStateFlow(false)
    private val appRulesReadFailed = MutableStateFlow(false)
    private val classificationStatsReadFailed = MutableStateFlow(false)

    private val notifications: StateFlow<List<NotificationItem>> =
        notificationRepository.observeNotifications()
            .withReadRecovery(notificationReadFailed)
            .map { items -> items.map(notificationContentPresenter::present) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList()
            )

    private val appRules: StateFlow<List<AppRule>> =
        notificationRepository.observeAppRules()
            .withReadRecovery(appRulesReadFailed)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList()
            )

    private val classificationStats: StateFlow<ClassificationStats> =
        notificationRepository.observeClassificationStats()
            .withReadRecovery(classificationStatsReadFailed)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                ClassificationStats()
            )

    val historyState: StateFlow<NotificationHistoryUiState> = combine(
        notifications,
        settingsRepository.settings,
        notificationAccessGranted,
        ingestionHealth,
        notificationReadFailed
    ) { items, settings, listenerGranted, health, readFailed ->
        NotificationHistoryUiState(
            items = items,
            selectedFilter = settings.selectedFilter,
            notificationAccessGranted = listenerGranted,
            ingestionHealth = health,
            readFailed = readFailed
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        NotificationHistoryUiState()
    )

    val settingsRulesState: StateFlow<SettingsRulesUiState> = combine(
        settingsRepository.settings,
        appRules,
        classificationStats,
        appRulesReadFailed,
        classificationStatsReadFailed
    ) { settings, currentRules, stats, rulesFailed, statsFailed ->
        SettingsRulesUiState(
            settings = settings,
            appRules = currentRules,
            classificationStats = stats,
            readFailed = rulesFailed || statsFailed
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        SettingsRulesUiState()
    )

    /** Compatibility aggregate for onboarding and the Phase 1 preview surface. */
    val state: StateFlow<AppState> = combine(
        historyState,
        settingsRulesState
    ) { history, settingsRules ->
        val settings = settingsRules.settings
        AppState(
            mode = settings.mode,
            preferencesLoaded = settings.preferencesLoaded,
            onboardingCompleted = settings.onboardingCompleted,
            notificationAccessGranted = history.notificationAccessGranted,
            digestSchedule = settings.digestSchedule,
            pausedUntilText = settings.pausedUntilText,
            items = history.items,
            appRules = settingsRules.appRules,
            classificationStats = settingsRules.classificationStats,
            ingestionHealth = history.ingestionHealth,
            selectedFilter = history.selectedFilter
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

    private fun <T> Flow<T>.withReadRecovery(
        failureState: MutableStateFlow<Boolean>
    ): Flow<T> =
        onEach { failureState.value = false }
            .retryWhen { error, attempt ->
                if (error is CancellationException) {
                    false
                } else {
                    failureState.value = true
                    delay(readRetryDelayMillis(attempt))
                    true
                }
            }

    private fun readRetryDelayMillis(attempt: Long): Long {
        val exponent = attempt.coerceAtMost(MAX_READ_RETRY_EXPONENT.toLong()).toInt()
        return (READ_RETRY_BASE_MILLIS shl exponent).coerceAtMost(READ_RETRY_MAX_MILLIS)
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

    private companion object {
        const val READ_RETRY_BASE_MILLIS = 250L
        const val READ_RETRY_MAX_MILLIS = 5_000L
        const val MAX_READ_RETRY_EXPONENT = 4
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
