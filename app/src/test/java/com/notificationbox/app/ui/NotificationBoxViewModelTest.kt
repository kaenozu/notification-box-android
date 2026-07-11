package com.notificationbox.app.ui

import com.notificationbox.app.data.NotificationStore
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.permission.FakePermissionStatusProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
        NotificationStore.clearAll()
        viewModel = NotificationBoxViewModel(fakeProvider)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when listener and post not granted, state reflects false`() = runTest {
        fakeProvider.listenerGranted = false
        fakeProvider.postGranted = false
        viewModel.refreshPermissions()

        val state = viewModel.state.first()
        assertEquals(false, state.notificationAccessGranted)
        assertEquals(false, state.postNotificationsGranted)
    }

    @Test
    fun `when both granted, state reflects true`() = runTest {
        fakeProvider.listenerGranted = true
        fakeProvider.postGranted = true
        viewModel.refreshPermissions()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(true, state.postNotificationsGranted)
    }

    @Test
    fun `when listener granted and post not, state is correct`() = runTest {
        fakeProvider.listenerGranted = true
        fakeProvider.postGranted = false
        viewModel.refreshPermissions()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(false, state.postNotificationsGranted)
    }

    @Test
    fun `permission refresh after revocation changes state`() = runTest {
        fakeProvider.listenerGranted = true
        fakeProvider.postGranted = true
        viewModel.refreshPermissions()
        assertEquals(true, viewModel.state.first().notificationAccessGranted)

        fakeProvider.listenerGranted = false
        viewModel.refreshPermissions()
        assertEquals(false, viewModel.state.first().notificationAccessGranted)
    }

    @Test
    fun `permission changes do not corrupt other state fields`() = runTest {
        fakeProvider.listenerGranted = true
        viewModel.refreshPermissions()

        val state = viewModel.state.first()
        assertEquals(true, state.notificationAccessGranted)
        assertEquals(com.notificationbox.app.model.AppMode.Observation, state.mode)
    }

    @Test
    fun `only opening settings does not grant permission`() = runTest {
        fakeProvider.listenerGranted = false
        fakeProvider.postGranted = false
        viewModel.refreshPermissions()

        assertEquals(false, viewModel.state.first().notificationAccessGranted)
        assertEquals(false, viewModel.state.first().postNotificationsGranted)
    }
}
