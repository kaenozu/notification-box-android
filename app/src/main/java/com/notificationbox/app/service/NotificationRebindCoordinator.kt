package com.notificationbox.app.service

import java.util.concurrent.atomic.AtomicBoolean

internal class NotificationRebindCoordinator(
    private val schedule: (Runnable, Long) -> Boolean,
    private val cancelScheduled: (Runnable) -> Unit,
    private val requestUnbind: () -> Unit,
    private val requestRebind: () -> Unit,
    private val recordFailure: () -> Unit,
    private val delayMillis: Long,
    private val requested: AtomicBoolean = AtomicBoolean(false)
) {
    fun request() {
        if (!requested.compareAndSet(false, true)) return

        val rebind = Runnable {
            try {
                requestRebind()
            } catch (_: Exception) {
                requested.set(false)
                recordFailure()
            }
        }

        try {
            check(schedule(rebind, delayMillis)) {
                "Unable to schedule notification-listener rebind"
            }
            requestUnbind()
        } catch (_: Exception) {
            cancelScheduled(rebind)
            requested.set(false)
            recordFailure()
        }
    }

    fun markConnected() {
        requested.set(false)
    }

    internal fun isRequested(): Boolean = requested.get()
}
