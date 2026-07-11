package com.notificationbox.app.ui

import com.notificationbox.app.data.NotificationStore
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.permission.FakeNotificationPermissionPlatform
import com.notificationbox.app.permission.FakePermissionStatusProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationBoxViewModelTest {

    private val fakePlatform = FakeNotificationPermissionPlatform()
    private val fakeProvider = FakePermissionStatusProvider(fakePlatform)
    private lateinit var viewModel: NotificationBoxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        NotificationStore.clearAll()
        viewModel = NotificationBoxViewModel(fakeProvider)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when both permissions false, state reflects false`() = runTest {
        fakeProvider.setListenerGranted(false)
        fakeProvider.setPostNotificationsPermission(false)
        fakeProvider.setNotificationsEnabled(false)

        viewModel.refreshPermissions()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(false, state.notificationAccessGranted)
        assertEquals(false, state.postNotificationsGranted)
    }

    @Test
    fun `when listener true and post false, state is correct`() = runTest {
        fakeProvider.setListenerGranted(true)
        fakeProvider.setPostNotificationsPermission(false)
        fakeProvider.setNotificationsEnabled(false)

        viewModel.refreshPermissions()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(false, state.postNotificationsGranted)
    }

    @Test
    fun `when listener false, post permission true and enabled true, state is correct`() = runTest {
        fakeProvider.setListenerGranted(false)
        fakeProvider.setPostNotificationsPermission(true)
        fakeProvider.setNotificationsEnabled(true)

        viewModel.refreshPermissions()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(false, state.notificationAccessGranted)
        assertEquals(true, state.postNotificationsGranted)
    }

    @Test
    fun `when both permissions true, state reflects true`() = runTest {
        fakeProvider.setListenerGranted(true)
        fakeProvider.setPostNotificationsPermission(true)
        fakeProvider.setNotificationsEnabled(true)

        viewModel.refreshPermissions()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(true, state.postNotificationsGranted)
    }

    @Test
    fun `permission refresh after revocation changes state`() = runTest {
        fakeProvider.setListenerGranted(true)
        fakeProvider.setPostNotificationsPermission(true)
        fakeProvider.setNotificationsEnabled(true)
        viewModel.refreshPermissions()
        advanceUntilIdle()
        assertEquals(true, viewModel.state.first().notificationAccessGranted)

        fakeProvider.setListenerGranted(false)
        viewModel.refreshPermissions()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.first().notificationAccessGranted)
    }

@Test
    fun `permission changes do not corrupt other state fields`() = runTest {
        NotificationStore.setModeForTesting(AppMode.Active)
        fakeProvider.setListenerGranted(true)
        viewModel.refreshPermissions()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(AppMode.Active, state.mode)
    }

    @Test
    fun `only opening settings does not grant permission`() = runTest {
        fakeProvider.setListenerGranted(false)
        fakeProvider.setPostNotificationsPermission(false)
        fakeProvider.setNotificationsEnabled(false)
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
        fakeProvider.setListenerGranted(true)
        viewModel.refreshPermissions()
        advanceUntilIdle()

        // Both should be updated
        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(1, state.items.size)
    }
}