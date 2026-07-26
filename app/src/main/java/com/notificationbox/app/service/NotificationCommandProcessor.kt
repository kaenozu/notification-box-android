package com.notificationbox.app.service

import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.model.IngestionErrorCode
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

internal sealed interface NotificationCommand {
    data class SynchronizeActive(
        val activeKeys: Set<String>,
        val notifications: List<NotificationRecord>,
        val synchronizedAtMillis: Long
    ) : NotificationCommand

    data class Upsert(
        val notification: NotificationRecord
    ) : NotificationCommand

    data class MarkRemoved(
        val key: String,
        val removedAtMillis: Long
    ) : NotificationCommand
}

internal class NotificationCommandProcessor(
    private val repository: NotificationRepository,
    private val healthReporter: NotificationIngestionHealthReporter,
    private val paymentSink: PaymentNotificationSink = NoOpPaymentNotificationSink
) {
    suspend fun process(command: NotificationCommand) {
        try {
            when (command) {
                is NotificationCommand.SynchronizeActive -> {
                    repository.synchronizeActive(
                        activeKeys = command.activeKeys,
                        notifications = command.notifications,
                        synchronizedAtMillis = command.synchronizedAtMillis
                    )
                    command.notifications.forEach { notification ->
                        paymentSink.capture(notification)
                    }
                }

                is NotificationCommand.Upsert -> {
                    repository.upsert(command.notification)
                    paymentSink.capture(command.notification)
                }

                is NotificationCommand.MarkRemoved -> repository.markRemoved(
                    key = command.key,
                    removedAtMillis = command.removedAtMillis
                )
            }
            healthReporter.recordSuccess()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            healthReporter.recordFailure(IngestionErrorCode.REPOSITORY_OPERATION_FAILED)
        }
    }
}

internal class NotificationCommandQueue(
    scope: CoroutineScope,
    private val processor: NotificationCommandProcessor,
    private val healthReporter: NotificationIngestionHealthReporter,
    capacity: Int = DEFAULT_CAPACITY,
    private val onOverflow: () -> Unit = {}
) {
    private val closed = AtomicBoolean(false)
    private val commands = Channel<NotificationCommand>(capacity = capacity.also {
        require(it > 0) { "Notification command queue capacity must be positive" }
    })
    private val consumerJob: Job = scope.launch {
        for (command in commands) {
            processor.process(command)
        }
    }

    fun submit(command: NotificationCommand): Boolean {
        if (closed.get()) {
            healthReporter.recordFailure(IngestionErrorCode.COMMAND_QUEUE_CLOSED)
            return false
        }

        if (commands.trySend(command).isSuccess) return true

        if (closed.get()) {
            healthReporter.recordFailure(IngestionErrorCode.COMMAND_QUEUE_CLOSED)
        } else {
            healthReporter.recordFailure(IngestionErrorCode.COMMAND_QUEUE_OVERFLOW)
            runCatching(onOverflow).onFailure {
                healthReporter.recordFailure(IngestionErrorCode.ACTIVE_SNAPSHOT_FAILED)
            }
        }
        return false
    }

    /** Stops new submissions while allowing all already-accepted commands to drain. */
    fun close() {
        if (closed.compareAndSet(false, true)) {
            commands.close()
        }
    }

    suspend fun join() {
        consumerJob.join()
    }

    companion object {
        internal const val DEFAULT_CAPACITY = 256
    }
}
