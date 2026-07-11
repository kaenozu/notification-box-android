package com.notificationbox.app.ui

import com.notificationbox.app.data.NotificationStore
import com.notificationbox.app.data.repository.FakeNotificationRepository
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import com.notificationbox.app.permission.FakePermissionStatusProvider
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationBoxViewModelTest {
    private lateinit var fakeProvider: FakePermissionStatusProvider
    private lateinit var fakeRepository: FakeNotificationRepository
    private lateinit var viewModel: NotificationBoxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        NotificationStore.setFilter(null)
        fakeProvider = FakePermissionStatusProvider()
        fakeRepository = FakeNotificationRepository()
        viewModel = NotificationBoxViewModel(fakeProvider, fakeRepository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `permission state reflects provider values`() = runTest {
        fakeProvider.listenerGranted = true
        fakeProvider.postNotificationsGranted = false

        viewModel.refreshPermissions()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(false, state.postNotificationsGranted)
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
    fun `repository flow is reflected in UI state`() = runTest {
        fakeRepository.emit(listOf(item("notification-key")))

        val state = viewModel.state.first()

        assertEquals(listOf("notification-key"), state.items.map { it.key })
    }

    @Test
    fun `permission refresh preserves notifications and filter`() = runTest {
        NotificationStore.setFilter(NotificationDecision.KeepNow)
        fakeRepository.emit(listOf(item("notification-key")))
        fakeProvider.listenerGranted = true

        viewModel.refreshPermissions()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(NotificationDecision.KeepNow, state.selectedFilter)
        assertEquals(listOf("notification-key"), state.items.map { it.key })
    }

    @Test
    fun `pin delete and clear operations are delegated to repository`() = runTest {
        fakeRepository.emit(listOf(item("notification-key")))

        viewModel.togglePinned("notification-key", true)
        viewModel.delete("notification-key")
        viewModel.clearAll()
        advanceUntilIdle()

        assertEquals(listOf("notification-key" to true), fakeRepository.pinnedUpdates)
        assertEquals(listOf("notification-key"), fakeRepository.deletedKeys)
        assertEquals(1, fakeRepository.clearAllCalls)
    }

    @Test
    fun `provider is read only at initialization and explicit refresh`() = runTest {
        assertEquals(1, fakeProvider.listenerCallCount)
        assertEquals(1, fakeProvider.postCallCount)

        fakeProvider.resetCallCounts()
        fakeRepository.emit(listOf(item("notification-key")))
        NotificationStore.setFilter(NotificationDecision.Ignore)
        viewModel.togglePinned("notification-key", true)
        viewModel.delete("notification-key")
        viewModel.clearAll()
        advanceUntilIdle()

        assertEquals(0, fakeProvider.listenerCallCount)
        assertEquals(0, fakeProvider.postCallCount)

        viewModel.refreshPermissions()

        assertEquals(1, fakeProvider.listenerCallCount)
        assertEquals(1, fakeProvider.postCallCount)
    }

    private fun item(key: String): NotificationItem =
        NotificationItem(
            key = key,
            packageName = "com.example.app",
            appLabel = "Example",
            title = "Title",
            text = "Text",
            postTime = Instant.ofEpochMilli(1_000),
            category = NotificationDecision.KeepNow,
            reason = "test"
        )
}
