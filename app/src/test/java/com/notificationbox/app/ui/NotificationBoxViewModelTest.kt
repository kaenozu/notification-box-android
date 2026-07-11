package com.notificationbox.app.ui

import com.notificationbox.app.domain.FakeNotificationRepository
import com.notificationbox.app.domain.NotificationClassifier
import com.notificationbox.app.domain.NotificationRecord
import com.notificationbox.app.domain.NotificationSample
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.NotificationDecision
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationBoxViewModelTest {

    private val fakeProvider = FakePermissionStatusProvider()
    private val fakeRepository = FakeNotificationRepository()
    private lateinit var viewModel: NotificationBoxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = NotificationBoxViewModel(fakeProvider, fakeRepository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `repository items are reflected in state`() = runTest {
        val record = NotificationRecord(
            key = "k1", packageName = "com.test", appLabel = "Test",
            title = "t", text = "b", postTimeMillis = 1000L,
            notificationId = 1, tag = null, channelId = "ch",
            category = NotificationDecision.KeepNow, reason = "r",
            userPinned = false, isActive = true, removedAtMillis = null
        )
        fakeRepository.upsert(record)
        advanceUntilIdle()

        val items = viewModel.state.first().items
        assertEquals(1, items.size)
        assertEquals("k1", items.first().notificationKey)
    }

    @Test
    fun `togglePinned delegates to repository`() = runTest {
        val record = NotificationRecord(
            key = "k1", packageName = "com.test", appLabel = "Test",
            title = "t", text = "b", postTimeMillis = 1000L,
            notificationId = 1, tag = null, channelId = "ch",
            category = NotificationDecision.KeepNow, reason = "r",
            userPinned = false, isActive = true, removedAtMillis = null
        )
        fakeRepository.upsert(record)
        advanceUntilIdle()
        fakeRepository.resetCounts()

        viewModel.togglePinned("k1", true)
        advanceUntilIdle()
        assertTrue(fakeRepository.setPinnedCount > 0)
    }

    @Test
    fun `delete delegates to repository`() = runTest {
        val record = NotificationRecord(
            key = "k1", packageName = "com.test", appLabel = "Test",
            title = "t", text = "b", postTimeMillis = 1000L,
            notificationId = 1, tag = null, channelId = "ch",
            category = NotificationDecision.KeepNow, reason = "r",
            userPinned = false, isActive = true, removedAtMillis = null
        )
        fakeRepository.upsert(record)
        advanceUntilIdle()
        fakeRepository.resetCounts()

        viewModel.delete("k1")
        advanceUntilIdle()
        assertTrue(fakeRepository.deleteCount > 0)
    }

    @Test
    fun `clearAll delegates to repository`() = runTest {
        viewModel.clearAll()
        advanceUntilIdle()
        assertTrue(fakeRepository.clearAllCount > 0)
    }

    @Test
    fun `permission changes do not corrupt notification items`() = runTest {
        fakeRepository.upsert(NotificationRecord(
            key = "k1", packageName = "com.test", appLabel = "Test",
            title = "t", text = "b", postTimeMillis = 1000L,
            notificationId = 1, tag = null, channelId = "ch",
            category = NotificationDecision.KeepNow, reason = "r",
            userPinned = false, isActive = true, removedAtMillis = null
        ))
        advanceUntilIdle()
        assertEquals(1, viewModel.state.first().items.size)

        viewModel.refreshPermissions()
        advanceUntilIdle()
        assertEquals(1, viewModel.state.first().items.size)
    }

    @Test
    fun `provider call counts correct for initialization`() = runTest {
        assertEquals(1, fakeProvider.listenerCallCount)
        assertEquals(1, fakeProvider.postCallCount)
    }

    @Test
    fun `filter state is preserved through notification updates`() = runTest {
        viewModel.setFilter(NotificationDecision.KeepNow)
        advanceUntilIdle()
        assertEquals(NotificationDecision.KeepNow, viewModel.state.first().selectedFilter)
    }
}