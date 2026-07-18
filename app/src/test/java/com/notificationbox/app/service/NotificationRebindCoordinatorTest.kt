package com.notificationbox.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRebindCoordinatorTest {
    @Test
    fun `duplicate requests schedule one rebind and connection clears watchdog`() {
        val scheduler = TestScheduler()
        var unbindCalls = 0
        var rebindCalls = 0
        var failures = 0
        val coordinator = NotificationRebindCoordinator(
            schedule = scheduler::schedule,
            cancelScheduled = scheduler::cancel,
            requestUnbind = { unbindCalls++ },
            requestRebind = { rebindCalls++ },
            recordFailure = { failures++ },
            delayMillis = 250L,
            connectionTimeoutMillis = 1_000L
        )

        coordinator.request()
        coordinator.request()

        assertEquals(1, scheduler.size)
        assertEquals(1, unbindCalls)
        assertTrue(coordinator.isRequested())

        scheduler.runNext(expectedDelayMillis = 250L)

        assertEquals(1, rebindCalls)
        assertEquals(1, scheduler.size)
        assertEquals(0, failures)
        assertTrue(coordinator.isRequested())

        coordinator.markConnected()

        assertEquals(0, scheduler.size)
        assertFalse(coordinator.isRequested())
        assertEquals(0, coordinator.attemptCount())
    }

    @Test
    fun `schedule rejection records failure and permits retry`() {
        var scheduleCalls = 0
        var cancellations = 0
        var failures = 0
        val coordinator = NotificationRebindCoordinator(
            schedule = { _, _ ->
                scheduleCalls++
                false
            },
            cancelScheduled = { cancellations++ },
            requestUnbind = { error("must not unbind") },
            requestRebind = { error("must not rebind") },
            recordFailure = { failures++ },
            delayMillis = 250L
        )

        coordinator.request()
        coordinator.request()

        assertEquals(2, scheduleCalls)
        assertEquals(2, cancellations)
        assertEquals(2, failures)
        assertFalse(coordinator.isRequested())
    }

    @Test
    fun `unbind failure cancels scheduled callback and permits retry`() {
        val scheduler = TestScheduler()
        var unbindCalls = 0
        var failures = 0
        val coordinator = NotificationRebindCoordinator(
            schedule = scheduler::schedule,
            cancelScheduled = scheduler::cancel,
            requestUnbind = {
                unbindCalls++
                error("forced unbind failure")
            },
            requestRebind = { error("must not rebind") },
            recordFailure = { failures++ },
            delayMillis = 250L
        )

        coordinator.request()
        coordinator.request()

        assertEquals(2, unbindCalls)
        assertEquals(2, failures)
        assertEquals(0, scheduler.size)
        assertFalse(coordinator.isRequested())
    }

    @Test
    fun `rebind failure clears request state and records failure`() {
        val scheduler = TestScheduler()
        var failures = 0
        val coordinator = NotificationRebindCoordinator(
            schedule = scheduler::schedule,
            cancelScheduled = scheduler::cancel,
            requestUnbind = {},
            requestRebind = { error("forced rebind failure") },
            recordFailure = { failures++ },
            delayMillis = 250L
        )

        coordinator.request()
        scheduler.runNext(expectedDelayMillis = 250L)

        assertEquals(1, failures)
        assertEquals(0, scheduler.size)
        assertFalse(coordinator.isRequested())
    }

    @Test
    fun `connection timeout retries with exponential backoff and stops at maximum`() {
        val scheduler = TestScheduler()
        var rebindCalls = 0
        var timeoutFailures = 0
        val coordinator = NotificationRebindCoordinator(
            schedule = scheduler::schedule,
            cancelScheduled = scheduler::cancel,
            requestUnbind = {},
            requestRebind = { rebindCalls++ },
            recordFailure = { error("unexpected generic failure") },
            recordTimeout = { timeoutFailures++ },
            delayMillis = 100L,
            connectionTimeoutMillis = 1_000L,
            maxAttempts = 3
        )

        coordinator.request()
        scheduler.runNext(expectedDelayMillis = 100L)
        scheduler.runNext(expectedDelayMillis = 1_000L)
        scheduler.runNext(expectedDelayMillis = 200L)
        scheduler.runNext(expectedDelayMillis = 1_000L)
        scheduler.runNext(expectedDelayMillis = 400L)
        scheduler.runNext(expectedDelayMillis = 1_000L)

        assertEquals(3, rebindCalls)
        assertEquals(3, timeoutFailures)
        assertEquals(0, scheduler.size)
        assertFalse(coordinator.isRequested())
        assertEquals(0, coordinator.attemptCount())
    }

    @Test
    fun `cancel removes pending rebind without recording failure`() {
        val scheduler = TestScheduler()
        var failures = 0
        val coordinator = NotificationRebindCoordinator(
            schedule = scheduler::schedule,
            cancelScheduled = scheduler::cancel,
            requestUnbind = {},
            requestRebind = {},
            recordFailure = { failures++ },
            delayMillis = 250L
        )

        coordinator.request()
        coordinator.cancel()

        assertEquals(0, scheduler.size)
        assertEquals(0, failures)
        assertFalse(coordinator.isRequested())
    }

    private class TestScheduler {
        private data class Entry(val runnable: Runnable, val delayMillis: Long)

        private val entries = mutableListOf<Entry>()
        val size: Int get() = entries.size

        fun schedule(runnable: Runnable, delayMillis: Long): Boolean {
            entries += Entry(runnable, delayMillis)
            return true
        }

        fun cancel(runnable: Runnable) {
            entries.removeAll { it.runnable === runnable }
        }

        fun runNext(expectedDelayMillis: Long) {
            val entry = entries.removeAt(0)
            assertEquals(expectedDelayMillis, entry.delayMillis)
            entry.runnable.run()
        }
    }
}
