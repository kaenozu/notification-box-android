package com.notificationbox.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRebindCoordinatorTest {
    @Test
    fun `duplicate requests schedule one rebind that survives request scope`() {
        var scheduled: Runnable? = null
        var scheduleCalls = 0
        var unbindCalls = 0
        var rebindCalls = 0
        var failures = 0
        val coordinator = NotificationRebindCoordinator(
            schedule = { runnable, _ ->
                scheduleCalls++
                scheduled = runnable
                true
            },
            cancelScheduled = {},
            requestUnbind = { unbindCalls++ },
            requestRebind = { rebindCalls++ },
            recordFailure = { failures++ },
            delayMillis = 250L
        )

        coordinator.request()
        coordinator.request()

        assertEquals(1, scheduleCalls)
        assertEquals(1, unbindCalls)
        assertNotNull(scheduled)
        assertTrue(coordinator.isRequested())

        scheduled?.run()

        assertEquals(1, rebindCalls)
        assertEquals(0, failures)
        assertTrue(coordinator.isRequested())

        coordinator.markConnected()
        assertFalse(coordinator.isRequested())
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
        var scheduled: Runnable? = null
        var cancellations = 0
        var unbindCalls = 0
        var failures = 0
        val coordinator = NotificationRebindCoordinator(
            schedule = { runnable, _ ->
                scheduled = runnable
                true
            },
            cancelScheduled = { runnable ->
                if (runnable === scheduled) cancellations++
            },
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
        assertEquals(2, cancellations)
        assertEquals(2, failures)
        assertFalse(coordinator.isRequested())
    }

    @Test
    fun `rebind failure clears request state and records failure`() {
        var scheduled: Runnable? = null
        var failures = 0
        val coordinator = NotificationRebindCoordinator(
            schedule = { runnable, _ ->
                scheduled = runnable
                true
            },
            cancelScheduled = {},
            requestUnbind = {},
            requestRebind = { error("forced rebind failure") },
            recordFailure = { failures++ },
            delayMillis = 250L
        )

        coordinator.request()
        scheduled?.run()

        assertEquals(1, failures)
        assertFalse(coordinator.isRequested())
    }
}
