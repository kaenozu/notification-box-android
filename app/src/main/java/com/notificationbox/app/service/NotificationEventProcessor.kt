package com.notificationbox.app.service

import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

internal enum class NotificationEventType {
    CONNECT_SYNC,
    POSTED,
    REMOVED
}

internal fun interface NotificationEventFailureReporter {
    fun report(eventType: NotificationEventType)
}

internal sealed interface NotificationRepositoryEvent {
    val type: NotificationEventType

    data class SynchronizeActive(
        val activeKeys: Set<String>,
        val notifications: List<NotificationRecord>,
        val synchronizedAtMillis: Long
    ) : NotificationRepositoryEvent {
        override val type: NotificationEventType = NotificationEventType.CONNECT_SYNC
    }

    data class Posted(
        val notification: NotificationRecord
    ) : NotificationRepositoryEvent {
        override val type: NotificationEventType = NotificationEventType.POSTED
    }

    data class Removed(
        val key: String,
        val removedAtMillis: Long
    ) : NotificationRepositoryEvent {
        override val type: NotificationEventType = NotificationEventType.REMOVED
    }
}

/**
 * Applies notification-listener events to the repository in callback arrival order.
 *
 * Android delivers listener callbacks on the app main thread. Callers enqueue immutable,
 * content-minimized repository events from that thread; one consumer performs all database work.
 */
internal class NotificationEventProcessor(
    private val repository: NotificationRepository,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val failureReporter: NotificationEventFailureReporter = NotificationEventFailureReporter { }
) {
    private val events = Channel<NotificationRepositoryEvent>(capacity = Channel.UNLIMITED)
    private val consumerJob: Job = scope.launch(dispatcher) {
        for (event in events) {
            processSafely(event)
        }
    }

    fun enqueue(event: NotificationRepositoryEvent): Boolean =
        events.trySend(event).isSuccess

    /** Stops accepting new events while allowing already-enqueued events to drain. */
    fun close() {
        events.close()
    }

    suspend fun join() {
        consumerJob.join()
    }

    private suspend fun processSafely(event: NotificationRepositoryEvent) {
        try {
            when (event) {
                is NotificationRepositoryEvent.SynchronizeActive -> repository.synchronizeActive(
                    activeKeys = event.activeKeys,
                    notifications = event.notifications,
                    synchronizedAtMillis = event.synchronizedAtMillis
                )
                is NotificationRepositoryEvent.Posted -> repository.upsert(event.notification)
                is NotificationRepositoryEvent.Removed -> repository.markRemoved(
                    key = event.key,
                    removedAtMillis = event.removedAtMillis
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Deliberately report only the event type. Notification content and identifiers stay private.
            failureReporter.report(event.type)
        }
    }
}
