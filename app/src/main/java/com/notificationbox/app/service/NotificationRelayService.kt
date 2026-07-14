package com.notificationbox.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notificationbox.app.App
import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationRelayService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clock: Clock = Clock.systemUTC()
    private var eventProcessor: NotificationEventProcessor? = null

    private val repository: NotificationRepository by lazy {
        (application as App).container.notificationRepository
    }

    private val recordFactory: NotificationRecordFactory by lazy {
        NotificationRecordFactory(
            ownPackageName = packageName,
            appLabelResolver = CachingAppLabelResolver(packageManager)
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val currentNotifications = runCatching {
            activeNotifications?.toList().orEmpty()
        }.getOrNull() ?: return

        val activeKeys = currentNotifications
            .asSequence()
            .filterNot { it.packageName == packageName }
            .map(StatusBarNotification::getKey)
            .toSet()
        val records = currentNotifications.mapNotNull(::createRecordSafely)
        enqueue(
            NotificationRepositoryEvent.SynchronizeActive(
                activeKeys = activeKeys,
                notifications = records,
                synchronizedAtMillis = clock.millis()
            )
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val record = createRecordSafely(sbn) ?: return
        enqueue(NotificationRepositoryEvent.Posted(record))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        enqueue(
            NotificationRepositoryEvent.Removed(
                key = sbn.key,
                removedAtMillis = clock.millis()
            )
        )
    }

    override fun onDestroy() {
        val processor = eventProcessor
        if (processor == null) {
            serviceScope.cancel()
        } else {
            processor.close()
            serviceScope.launch {
                processor.join()
                serviceScope.cancel()
            }
        }
        super.onDestroy()
    }

    private fun createRecordSafely(sbn: StatusBarNotification): NotificationRecord? =
        runCatching { recordFactory.create(sbn) }.getOrNull()

    private fun enqueue(event: NotificationRepositoryEvent) {
        processor().enqueue(event)
    }

    private fun processor(): NotificationEventProcessor =
        eventProcessor ?: NotificationEventProcessor(
            repository = repository,
            scope = serviceScope
        ).also { eventProcessor = it }
}
