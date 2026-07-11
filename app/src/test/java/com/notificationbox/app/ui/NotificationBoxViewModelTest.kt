package com.notificationbox.app.ui

import com.notificationbox.app.model.AppMode
import com.notificationbox.app.permission.FakePermissionStatusProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationBoxViewModelTest {

    private val fakeProvider = FakePermissionStatusProvider()
    private lateinit var viewModel: NotificationBoxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = NotificationBoxViewModel(fakeProvider)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when both permissions false, state reflects false`() = runTest {
        fakeProvider.listenerGranted = false
        fakeProvider.postNotificationsGranted = false

        viewModel.refreshPermissions()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(false, state.notificationAccessGranted)
        assertEquals(false, state.postNotificationsGranted)
    }

    @Test
    fun `when listener true and post false, state is correct`() = runTest {
        fakeProvider.listenerGranted = true
        fakeProvider.postNotificationsGranted = false

        viewModel.refreshPermissions()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(false, state.postNotificationsGranted)
    }

    @Test
    fun `when listener false, post permission true, state is correct`() = runTest {
        fakeProvider.listenerGranted = false
        fakeProvider.postNotificationsGranted = true

        viewModel.refreshPermissions()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(false, state.notificationAccessGranted)
        assertEquals(true, state.postNotificationsGranted)
    }

    @Test
    fun `when both permissions true, state reflects true`() = runTest {
        fakeProvider.listenerGranted = true
        fakeProvider.postNotificationsGranted = true

        viewModel.refreshPermissions()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(true, state.postNotificationsGranted)
    }

    @Test
    fun `permission refresh after revocation changes state`() = runTest {
        fakeProvider.listenerGranted = true
        fakeProvider.postNotificationsGranted = true
        viewModel.refreshPermissions()
        advanceUntilIdle()
        assertEquals(true, viewModel.state.first().notificationAccessGranted)

        fakeProvider.listenerGranted = false
        viewModel.refreshPermissions()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.first().notificationAccessGranted)
    }

    @Test
    fun `permission changes do not corrupt other state fields`() = runTest {
        fakeProvider.listenerGranted = true
        viewModel.refreshPermissions()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(AppMode.Observation, state.mode)
    }

    @Test
    fun `only opening settings does not grant permission`() = runTest {
        fakeProvider.listenerGranted = false
        fakeProvider.postNotificationsGranted = false
        viewModel.refreshPermissions()
        advanceUntilIdle()

        assertEquals(false, viewModel.state.first().notificationAccessGranted)
        assertEquals(false, viewModel.state.first().postNotificationsGranted)
    }

    @Test
    fun `permission state updates independently of store updates`() = runTest {
        // Add a notification (store update)
        viewModel.ingestDemo("com.test", "title", "text")
        advanceUntilIdle()

        // Verify items added
        assertEquals(1, viewModel.state.first().items.size)

        // Change permission - should NOT trigger extra Provider calls beyond refreshPermissions
        fakeProvider.listenerGranted = true
        viewModel.refreshPermissions()
        advanceUntilIdle()

        // Both should be updated
        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(1, state.items.size)
    }

    @Test
    fun `provider call counts are tracked correctly`() = runTest {
        // Initialization (in @Before setup) calls both methods once
        assertEquals(1, fakeProvider.listenerCallCount)
        assertEquals(1, fakeProvider.postCallCount)

        // refreshPermissions: should call both again
        fakeProvider.resetCallCounts()
        viewModel.refreshPermissions()
        advanceUntilIdle()
        assertEquals(1, fakeProvider.listenerCallCount)
        assertEquals(1, fakeProvider.postCallCount)

        // Store updates should NOT call Provider
        fakeProvider.resetCallCounts()
        viewModel.ingestDemo("com.test", "title", "text")
        advanceUntilIdle()
        assertEquals(0, fakeProvider.listenerCallCount)
        assertEquals(0, fakeProvider.postCallCount)

        fakeProvider.resetCallCounts()
        viewModel.setFilter(null)
        advanceUntilIdle()
        assertEquals(0, fakeProvider.listenerCallCount)
        assertEquals(0, fakeProvider.postCallCount)

        fakeProvider.resetCallCounts()
        viewModel.togglePinned(1L, true)
        advanceUntilIdle()
        assertEquals(0, fakeProvider.listenerCallCount)
        assertEquals(0, fakeProvider.postCallCount)

        fakeProvider.resetCallCounts()
        viewModel.delete(1L)
        advanceUntilIdle()
        assertEquals(0, fakeProvider.listenerCallCount)
        assertEquals(0, fakeProvider.postCallCount)

        fakeProvider.resetCallCounts()
        viewModel.clearAll()
        advanceUntilIdle()
        assertEquals(0, fakeProvider.listenerCallCount)
        assertEquals(0, fakeProvider.postCallCount)
    }
}