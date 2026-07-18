package com.notificationbox.app.service

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class NotificationRebindCoordinator(
    private val schedule: (Runnable, Long) -> Boolean,
    private val cancelScheduled: (Runnable) -> Unit,
    private val requestUnbind: () -> Unit,
    private val requestRebind: () -> Unit,
    private val recordFailure: () -> Unit,
    private val delayMillis: Long,
    private val connectionTimeoutMillis: Long = DEFAULT_CONNECTION_TIMEOUT_MILLIS,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val recordTimeout: () -> Unit = recordFailure,
    private val requested: AtomicBoolean = AtomicBoolean(false),
    private val attempt: AtomicInteger = AtomicInteger(0)
) {
    private val callbackLock = Any()
    private var scheduledRebind: Runnable? = null
    private var connectionWatchdog: Runnable? = null

    init {
        require(delayMillis >= 0) { "Rebind delay must not be negative" }
        require(connectionTimeoutMillis > 0) { "Connection timeout must be positive" }
        require(maxAttempts > 0) { "Maximum rebind attempts must be positive" }
    }

    fun request() {
        if (!requested.compareAndSet(false, true)) return
        attempt.set(0)
        scheduleRebind(unbindFirst = true)
    }

    fun markConnected() {
        reset(cancelCallbacks = true)
    }

    fun cancel() {
        reset(cancelCallbacks = true)
    }

    private fun scheduleRebind(unbindFirst: Boolean) {
        val currentAttempt = attempt.get()
        lateinit var rebind: Runnable
        rebind = Runnable {
            clearScheduledRebind(rebind)
            if (!requested.get()) return@Runnable

            try {
                requestRebind()
                scheduleConnectionWatchdog()
            } catch (_: Exception) {
                failAndReset(recordFailure)
            }
        }

        synchronized(callbackLock) {
            scheduledRebind = rebind
        }

        try {
            check(schedule(rebind, backoffDelayMillis(currentAttempt))) {
                "Unable to schedule notification-listener rebind"
            }
            if (unbindFirst) {
                requestUnbind()
            }
        } catch (_: Exception) {
            cancelScheduled(rebind)
            clearScheduledRebind(rebind)
            failAndReset(recordFailure)
        }
    }

    private fun scheduleConnectionWatchdog() {
        lateinit var watchdog: Runnable
        watchdog = Runnable {
            clearConnectionWatchdog(watchdog)
            if (!requested.get()) return@Runnable

            recordTimeout()
            val nextAttempt = attempt.incrementAndGet()
            if (nextAttempt >= maxAttempts) {
                reset(cancelCallbacks = false)
            } else {
                scheduleRebind(unbindFirst = false)
            }
        }

        synchronized(callbackLock) {
            connectionWatchdog = watchdog
        }

        try {
            check(schedule(watchdog, connectionTimeoutMillis)) {
                "Unable to schedule notification-listener connection watchdog"
            }
        } catch (_: Exception) {
            cancelScheduled(watchdog)
            clearConnectionWatchdog(watchdog)
            failAndReset(recordFailure)
        }
    }

    private fun backoffDelayMillis(currentAttempt: Int): Long {
        val exponent = currentAttempt.coerceAtMost(MAX_BACKOFF_EXPONENT)
        return (delayMillis shl exponent).coerceAtMost(MAX_REBIND_DELAY_MILLIS)
    }

    private fun failAndReset(recorder: () -> Unit) {
        reset(cancelCallbacks = true)
        recorder()
    }

    private fun reset(cancelCallbacks: Boolean) {
        val callbacks = synchronized(callbackLock) {
            val current = listOfNotNull(scheduledRebind, connectionWatchdog)
            scheduledRebind = null
            connectionWatchdog = null
            current
        }
        if (cancelCallbacks) {
            callbacks.forEach(cancelScheduled)
        }
        attempt.set(0)
        requested.set(false)
    }

    private fun clearScheduledRebind(runnable: Runnable) {
        synchronized(callbackLock) {
            if (scheduledRebind === runnable) {
                scheduledRebind = null
            }
        }
    }

    private fun clearConnectionWatchdog(runnable: Runnable) {
        synchronized(callbackLock) {
            if (connectionWatchdog === runnable) {
                connectionWatchdog = null
            }
        }
    }

    internal fun isRequested(): Boolean = requested.get()
    internal fun attemptCount(): Int = attempt.get()

    private companion object {
        const val DEFAULT_CONNECTION_TIMEOUT_MILLIS = 5_000L
        const val DEFAULT_MAX_ATTEMPTS = 3
        const val MAX_BACKOFF_EXPONENT = 4
        const val MAX_REBIND_DELAY_MILLIS = 10_000L
    }
}
