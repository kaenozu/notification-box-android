package com.notificationbox.app.ui

import com.notificationbox.app.data.NotificationStore
import com.notificationbox.app.data.repository.FakeNotificationRepository
import com.notificationbox.app.domain.dryrun.OrganizationMode
import com.notificationbox.app.domain.dryrun.PlannedAction
import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import com.notificationbox.app.permission.FakePermissionStatusProvider
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class NotificationBoxDryRunViewModelTest {
    private lateinit var repository: FakeNotificationRepository
    private lateinit var viewModel: NotificationBoxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        NotificationStore.setFilter(null)
        repository = FakeNotificationRepository()
        viewModel = NotificationBoxViewModel(FakePermissionStatusProvider(), repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `session starts observe only and counts active notifications`() = runTest {
        repository.emit(listOf(item("active"), item("inactive", isActive = false)))
        advanceUntilIdle()

        val state = viewModel.dryRunState.value
        assertEquals(OrganizationMode.OBSERVE_ONLY, state.mode)
        assertEquals(1, state.preview.activeNotificationCount)
        assertTrue(state.preview.plannedActions.isEmpty())
    }

    @Test
    fun `dry run follows resolved decisions without changing legacy mode`() = runTest {
        val legacyMode = NotificationStore.state.value.mode
        repository.emit(
            listOf(
                item("digest", NotificationDecision.HoldForDigest, DecisionSource.AppRule),
                item("ignore", NotificationDecision.Ignore, DecisionSource.UserOverride)
            )
        )
        viewModel.setOrganizationMode(OrganizationMode.DRY_RUN)
        advanceUntilIdle()

        val preview = viewModel.dryRunState.value.preview
        assertEquals(2, preview.activeNotificationCount)
        assertEquals(1, preview.countsByAction[PlannedAction.ADD_TO_DIGEST_PREVIEW])
        assertEquals(1, preview.countsByAction[PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW])
        assertEquals(legacyMode, NotificationStore.state.value.mode)
    }

    @Test
    fun `new view model session resets to observe only`() = runTest {
        viewModel.setOrganizationMode(OrganizationMode.DRY_RUN)
        assertEquals(OrganizationMode.DRY_RUN, viewModel.dryRunState.value.mode)

        val nextSession = NotificationBoxViewModel(FakePermissionStatusProvider(), repository)
        advanceUntilIdle()

        assertEquals(OrganizationMode.OBSERVE_ONLY, nextSession.dryRunState.value.mode)
    }

    private fun item(
        key: String,
        decision: NotificationDecision = NotificationDecision.KeepNow,
        source: DecisionSource = DecisionSource.Automatic,
        isActive: Boolean = true
    ): NotificationItem = NotificationItem(
        key = key,
        packageName = "com.example.app",
        appLabel = "Example",
        title = "Title",
        text = "Text",
        postTime = Instant.ofEpochMilli(1_000),
        automaticDecision = decision,
        userDecision = if (source == DecisionSource.UserOverride) decision else null,
        appRuleDecision = if (source == DecisionSource.AppRule) decision else null,
        category = decision,
        decisionSource = source,
        automaticReason = "test",
        reason = "test",
        isActive = isActive,
        removedAt = if (isActive) null else Instant.ofEpochMilli(2_000)
    )
}
