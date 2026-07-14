package com.notificationbox.app.service

import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.model.IngestionErrorCode
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
    private val healthReporter: NotificationIngestionHealthReporter
) {
    suspend fun process(command: NotificationCommand) {
        try {
            when (command) {
                is NotificationCommand.SynchronizeActive -> repository.synchronizeActive(
                    activeKeys = command.activeKeys,
                    notifications = command.notifications,
                    synchronizedAtMillis = command.synchronizedAtMillis
                )

                is NotificationCommand.Upsert -> repository.upsert(command.notification)
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
    private val healthReporter: NotificationIngestionHealthReporter
) {
    private val commands = Channel<NotificationCommand>(capacity = Channel.UNLIMITED)
    private val consumerJob: Job = scope.launch {
        for (command in commands) {
            processor.process(command)
        }
    }

    fun submit(command: NotificationCommand): Boolean {
        val accepted = commands.trySend(command).isSuccess
        if (!accepted) {
            healthReporter.recordFailure(IngestionErrorCode.COMMAND_QUEUE_CLOSED)
        }
        return accepted
    }

    /** Stops new submissions while allowing all already-accepted commands to drain. */
    fun close() {
        commands.close()
    }

    suspend fun join() {
        consumerJob.join()
    }
}
