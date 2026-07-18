package com.notificationbox.app.ui.summary

import com.notificationbox.app.data.repository.NotificationSummarySource
import com.notificationbox.app.model.NotificationSummary
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
class NotificationSummaryViewModelTest {
    private val now = Instant.parse("2026-07-17T00:00:00Z")

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loading becomes content for a non-empty summary`() = runTest {
        val summaries = MutableStateFlow(summary(total = 2))
        val viewModel = NotificationSummaryViewModel(
            summarySource = NotificationSummarySource { summaries },
            clock = Clock.fixed(now, ZoneOffset.UTC)
        )

        assertEquals(NotificationSummaryUiState.Loading, viewModel.uiState.value)

        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is NotificationSummaryUiState.Content)
        assertEquals(2, (state as NotificationSummaryUiState.Content).summary.totalCount)
        collection.cancel()
    }

    @Test
    fun `loading becomes empty for a zero summary`() = runTest {
        val viewModel = NotificationSummaryViewModel(
            summarySource = NotificationSummarySource { MutableStateFlow(summary(total = 0)) },
            clock = Clock.fixed(now, ZoneOffset.UTC)
        )

        assertEquals(NotificationSummaryUiState.Loading, viewModel.uiState.value)

        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(NotificationSummaryUiState.Empty, viewModel.uiState.value)
        collection.cancel()
    }

    @Test
    fun `loading becomes error when the source fails`() = runTest {
        val viewModel = NotificationSummaryViewModel(
            summarySource = NotificationSummarySource {
                flow { throw IllegalStateException("private diagnostic") }
            },
            clock = Clock.fixed(now, ZoneOffset.UTC)
        )

        assertEquals(NotificationSummaryUiState.Loading, viewModel.uiState.value)

        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(NotificationSummaryUiState.Error, viewModel.uiState.value)
        collection.cancel()
    }

    @Test
    fun `refresh can recover after a source failure`() = runTest {
        val clock = MutableClock(now)
        var fail = true
        val requestedPeriods = mutableListOf<Instant>()
        val viewModel = NotificationSummaryViewModel(
            summarySource = NotificationSummarySource { since ->
                requestedPeriods += since
                if (fail) {
                    flow { throw IllegalStateException("temporary failure") }
                } else {
                    flowOf(summary(total = 1).copy(periodStart = since))
                }
            },
            clock = clock
        )
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()
        assertEquals(NotificationSummaryUiState.Error, viewModel.uiState.value)

        fail = false
        clock.current = now.plusSeconds(60)
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is NotificationSummaryUiState.Content)
        assertEquals(2, requestedPeriods.size)
        assertEquals(now.minusSeconds(24 * 60 * 60), requestedPeriods.first())
        assertEquals(now.plusSeconds(60).minusSeconds(24 * 60 * 60), requestedPeriods.last())
        collection.cancel()
    }

    private fun summary(total: Int) = NotificationSummary(
        totalCount = total,
        keepNowCount = total,
        holdForDigestCount = 0,
        ignoreCount = 0,
        periodStart = now.minusSeconds(24 * 60 * 60),
        generatedAt = now
    )

    private class MutableClock(var current: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = current
    }
}
