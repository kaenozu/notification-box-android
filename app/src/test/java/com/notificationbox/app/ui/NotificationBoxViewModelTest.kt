package com.notificationbox.app.ui

import com.notificationbox.app.data.repository.FakeNotificationRepository
import com.notificationbox.app.data.settings.FakeSettingsRepository
import com.notificationbox.app.domain.dryrun.OrganizationMode
import com.notificationbox.app.domain.dryrun.PlannedAction
import com.notificationbox.app.model.AppRule
import com.notificationbox.app.model.ClassificationStats
import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.IngestionErrorCode
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationIngestionHealth
import com.notificationbox.app.model.NotificationItem
import com.notificationbox.app.permission.FakePermissionStatusProvider
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationBoxViewModelTest {
    private lateinit var provider: FakePermissionStatusProvider
    private lateinit var repository: FakeNotificationRepository
    private lateinit var settings: FakeSettingsRepository
    private lateinit var health: MutableStateFlow<NotificationIngestionHealth>
    private lateinit var viewModel: NotificationBoxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        provider = FakePermissionStatusProvider()
        repository = FakeNotificationRepository()
        settings = FakeSettingsRepository()
        health = MutableStateFlow(NotificationIngestionHealth())
        viewModel = createViewModel()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `permission state changes only on explicit refresh`() = runTest {
        assertEquals(false, viewModel.state.value.notificationAccessGranted)
        provider.listenerGranted = true
        assertEquals(false, viewModel.state.value.notificationAccessGranted)

        viewModel.refreshPermissions()

        assertEquals(true, viewModel.state.value.notificationAccessGranted)
    }

    @Test
    fun `repository state is reflected`() = runTest {
        repository.emit(listOf(item("one")))
        repository.emitRules(
            listOf(
                AppRule(
                    packageName = "com.example",
                    appLabel = "Example",
                    decision = NotificationDecision.Ignore,
                    updatedAt = Instant.EPOCH
                )
            )
        )
        repository.emitStats(ClassificationStats(automaticallyClassified = 4))
        health.value = NotificationIngestionHealth(
            processedCommands = 5,
            failedCommands = 1,
            lastError = IngestionErrorCode.REPOSITORY_OPERATION_FAILED
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf("one"), state.items.map(NotificationItem::key))
        assertEquals(1, state.appRules.size)
        assertEquals(4, state.classificationStats.automaticallyClassified)
        assertEquals(5, state.ingestionHealth.processedCommands)
    }

    @Test
    fun `settings are delegated without global state`() = runTest {
        viewModel.completeOnboarding()
        viewModel.setFilter(NotificationDecision.Ignore)
        viewModel.pause("明日まで")
        viewModel.setDigestHours(listOf(9, 18))
        advanceUntilIdle()

        val value = settings.settings.value
        assertTrue(value.onboardingCompleted)
        assertEquals(NotificationDecision.Ignore, value.selectedFilter)
        assertEquals("明日まで", value.pausedUntilText)
        assertEquals(listOf(9, 18), value.digestSchedule.hours)
    }

    @Test
    fun `notification mutations are delegated`() = runTest {
        viewModel.togglePinned("one", true)
        viewModel.setNotificationDecision("one", NotificationDecision.Ignore)
        viewModel.setAppRule("com.example", "Example", NotificationDecision.HoldForDigest)
        viewModel.delete("one")
        viewModel.clearAll()
        viewModel.resetClassificationStats()
        advanceUntilIdle()

        assertEquals(listOf("one" to true), repository.pinnedUpdates)
        assertEquals(
            listOf("one" to NotificationDecision.Ignore),
            repository.decisionUpdates
        )
        assertEquals(1, repository.appRuleUpdates.size)
        assertEquals(listOf("one"), repository.deletedKeys)
        assertEquals(1, repository.clearAllCalls)
        assertEquals(1, repository.resetStatsCalls)
    }

    @Test
    fun `dry run is session local and follows active notifications`() = runTest {
        repository.emit(
            listOf(
                item(
                    key = "digest",
                    decision = NotificationDecision.HoldForDigest,
                    source = DecisionSource.AppRule
                ),
                item(
                    key = "ignored",
                    decision = NotificationDecision.Ignore,
                    source = DecisionSource.UserOverride
                ),
                item("inactive", isActive = false)
            )
        )
        viewModel.setOrganizationMode(OrganizationMode.DRY_RUN)
        advanceUntilIdle()

        val preview = viewModel.dryRunState.value.preview
        assertEquals(2, preview.activeNotificationCount)
        assertEquals(1, preview.countsByAction[PlannedAction.ADD_TO_DIGEST_PREVIEW])
        assertEquals(1, preview.countsByAction[PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW])
        assertEquals(OrganizationMode.DRY_RUN, viewModel.dryRunState.value.mode)
        assertEquals(OrganizationMode.OBSERVE_ONLY, createViewModel().dryRunState.value.mode)
    }

    private fun createViewModel() = NotificationBoxViewModel(
        permissionProvider = provider,
        notificationRepository = repository,
        settingsRepository = settings,
        ingestionHealth = health
    )

    private fun item(
        key: String,
        decision: NotificationDecision = NotificationDecision.KeepNow,
        source: DecisionSource = DecisionSource.Automatic,
        isActive: Boolean = true
    ) = NotificationItem(
        key = key,
        packageName = "com.example",
        appLabel = "Example",
        title = "Title",
        text = "Text",
        postTime = Instant.EPOCH,
        automaticDecision = decision,
        userDecision = decision.takeIf { source == DecisionSource.UserOverride },
        appRuleDecision = decision.takeIf { source == DecisionSource.AppRule },
        category = decision,
        decisionSource = source,
        automaticReason = "test",
        reason = "test",
        isActive = isActive,
        removedAt = if (isActive) null else Instant.ofEpochMilli(1)
    )
}
