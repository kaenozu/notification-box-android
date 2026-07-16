package com.notificationbox.app.ui

import com.notificationbox.app.data.NotificationStore
import com.notificationbox.app.data.repository.FakeNotificationRepository
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
import kotlinx.coroutines.flow.first
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
    private lateinit var fakeProvider: FakePermissionStatusProvider
    private lateinit var fakeRepository: FakeNotificationRepository
    private lateinit var health: MutableStateFlow<NotificationIngestionHealth>
    private lateinit var viewModel: NotificationBoxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        NotificationStore.setFilter(null)
        fakeProvider = FakePermissionStatusProvider()
        fakeRepository = FakeNotificationRepository()
        health = MutableStateFlow(NotificationIngestionHealth())
        viewModel = NotificationBoxViewModel(
            permissionProvider = fakeProvider,
            notificationRepository = fakeRepository,
            ingestionHealth = health
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `permission state reflects listener grant`() = runTest {
        fakeProvider.listenerGranted = true

        viewModel.refreshPermissions()

        assertEquals(true, viewModel.state.first().notificationAccessGranted)
    }

    @Test
    fun `permission revocation is reflected`() = runTest {
        fakeProvider.listenerGranted = true
        viewModel.refreshPermissions()
        assertEquals(true, viewModel.state.first().notificationAccessGranted)

        fakeProvider.listenerGranted = false
        viewModel.refreshPermissions()
        assertEquals(false, viewModel.state.first().notificationAccessGranted)
    }

    @Test
    fun `repository notifications rules stats and health are reflected`() = runTest {
        fakeRepository.emit(listOf(item("notification-key")))
        fakeRepository.emitRules(
            listOf(
                AppRule(
                    packageName = "com.example.app",
                    appLabel = "Example",
                    decision = NotificationDecision.Ignore,
                    updatedAt = Instant.EPOCH
                )
            )
        )
        fakeRepository.emitStats(
            ClassificationStats(
                automaticallyClassified = 5,
                userOverrideChanges = 2,
                appRuleChanges = 1
            )
        )
        health.value = NotificationIngestionHealth(
            processedCommands = 8,
            failedCommands = 1,
            lastError = IngestionErrorCode.REPOSITORY_OPERATION_FAILED
        )

        val state = viewModel.state.first()
        assertEquals(listOf("notification-key"), state.items.map { it.key })
        assertEquals(listOf("com.example.app"), state.appRules.map { it.packageName })
        assertEquals(5, state.classificationStats.automaticallyClassified)
        assertEquals(2, state.classificationStats.userOverrideChanges)
        assertEquals(1, state.classificationStats.appRuleChanges)
        assertEquals(8, state.ingestionHealth.processedCommands)
        assertEquals(1, state.ingestionHealth.failedCommands)
    }

    @Test
    fun `permission refresh preserves notifications rules and filter`() = runTest {
        NotificationStore.setFilter(NotificationDecision.KeepNow)
        fakeRepository.emit(listOf(item("notification-key")))
        fakeRepository.emitRules(
            listOf(
                AppRule(
                    "com.example.app",
                    "Example",
                    NotificationDecision.Ignore,
                    Instant.EPOCH
                )
            )
        )
        fakeProvider.listenerGranted = true

        viewModel.refreshPermissions()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(NotificationDecision.KeepNow, state.selectedFilter)
        assertEquals(listOf("notification-key"), state.items.map { it.key })
        assertEquals(1, state.appRules.size)
    }

    @Test
    fun `notification and app rule decisions are delegated to repository`() = runTest {
        fakeRepository.emit(listOf(item("notification-key")))

        viewModel.setNotificationDecision(
            "notification-key",
            NotificationDecision.Ignore
        )
        viewModel.setAppRule(
            "com.example.app",
            "Example",
            NotificationDecision.HoldForDigest
        )
        advanceUntilIdle()

        assertEquals(
            listOf("notification-key" to NotificationDecision.Ignore),
            fakeRepository.decisionUpdates
        )
        assertEquals(
            listOf(
                Triple(
                    "com.example.app",
                    "Example",
                    NotificationDecision.HoldForDigest
                )
            ),
            fakeRepository.appRuleUpdates
        )
    }

    @Test
    fun `pin delete clear and stats reset are delegated`() = runTest {
        fakeRepository.emit(listOf(item("notification-key")))

        viewModel.togglePinned("notification-key", true)
        viewModel.delete("notification-key")
        viewModel.clearAll()
        viewModel.resetClassificationStats()
        advanceUntilIdle()

        assertEquals(
            listOf("notification-key" to true),
            fakeRepository.pinnedUpdates
        )
        assertEquals(
            listOf("notification-key"),
            fakeRepository.deletedKeys
        )
        assertEquals(1, fakeRepository.clearAllCalls)
        assertEquals(1, fakeRepository.resetStatsCalls)
    }

    @Test
    fun `provider is read only at initialization and explicit refresh`() = runTest {
        assertEquals(1, fakeProvider.listenerCallCount)
        fakeProvider.resetCallCounts()

        fakeRepository.emit(listOf(item("notification-key")))
        NotificationStore.setFilter(NotificationDecision.Ignore)
        viewModel.togglePinned("notification-key", true)
        viewModel.setNotificationDecision(
            "notification-key",
            NotificationDecision.Ignore
        )
        viewModel.setAppRule(
            "com.example.app",
            "Example",
            NotificationDecision.Ignore
        )
        viewModel.delete("notification-key")
        viewModel.clearAll()
        advanceUntilIdle()

        assertEquals(0, fakeProvider.listenerCallCount)

        viewModel.refreshPermissions()

        assertEquals(1, fakeProvider.listenerCallCount)
    }

    @Test
    fun `dry run session starts observe only and derives active count`() = runTest {
        fakeRepository.emit(
            listOf(
                item("active", decision = NotificationDecision.HoldForDigest),
                item("inactive", isActive = false)
            )
        )
        advanceUntilIdle()

        val dryRunState = viewModel.dryRunState.value
        assertEquals(OrganizationMode.OBSERVE_ONLY, dryRunState.mode)
        assertEquals(1, dryRunState.preview.activeNotificationCount)
        assertTrue(dryRunState.preview.plannedActions.isEmpty())
    }

    @Test
    fun `dry run preview follows repository without legacy mode mutation`() = runTest {
        val legacyModeBefore = NotificationStore.state.value.mode
        fakeRepository.emit(
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
                )
            )
        )

        viewModel.setOrganizationMode(OrganizationMode.DRY_RUN)
        advanceUntilIdle()

        val firstPreview = viewModel.dryRunState.value.preview
        assertEquals(2, firstPreview.activeNotificationCount)
        assertEquals(
            1,
            firstPreview.countsByAction[PlannedAction.ADD_TO_DIGEST_PREVIEW]
        )
        assertEquals(
            1,
            firstPreview.countsByAction[PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW]
        )
        assertEquals(legacyModeBefore, NotificationStore.state.value.mode)

        fakeRepository.emit(
            listOf(item("keep", decision = NotificationDecision.KeepNow))
        )
        advanceUntilIdle()

        val updatedPreview = viewModel.dryRunState.value.preview
        assertEquals(1, updatedPreview.activeNotificationCount)
        assertEquals(
            1,
            updatedPreview.countsByAction[PlannedAction.KEEP_IN_CURRENT_VIEW]
        )
        assertEquals(legacyModeBefore, NotificationStore.state.value.mode)
    }

    @Test
    fun `new view model session resets organization mode to observe only`() = runTest {
        viewModel.setOrganizationMode(OrganizationMode.DRY_RUN)
        assertEquals(
            OrganizationMode.DRY_RUN,
            viewModel.dryRunState.value.mode
        )

        val newSession = NotificationBoxViewModel(
            permissionProvider = fakeProvider,
            notificationRepository = fakeRepository,
            ingestionHealth = health
        )
        advanceUntilIdle()

        assertEquals(
            OrganizationMode.OBSERVE_ONLY,
            newSession.dryRunState.value.mode
        )
    }

    private fun item(
        key: String,
        decision: NotificationDecision = NotificationDecision.KeepNow,
        source: DecisionSource = DecisionSource.Automatic,
        isActive: Boolean = true
    ): NotificationItem =
        NotificationItem(
            key = key,
            packageName = "com.example.app",
            appLabel = "Example",
            title = "Title",
            text = "Text",
            postTime = Instant.ofEpochMilli(1_000),
            automaticDecision = decision,
            userDecision =
                if (source == DecisionSource.UserOverride) decision else null,
            appRuleDecision =
                if (source == DecisionSource.AppRule) decision else null,
            category = decision,
            decisionSource = source,
            automaticReason = "test",
            reason = "test",
            isActive = isActive,
            removedAt =
                if (isActive) null else Instant.ofEpochMilli(2_000)
        )
}
