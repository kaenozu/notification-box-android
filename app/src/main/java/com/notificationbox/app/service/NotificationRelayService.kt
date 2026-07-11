package com.notificationbox.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notificationbox.app.App
import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationRelayService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clock: Clock = Clock.systemUTC()

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

        val records = currentNotifications.mapNotNull(::createRecordSafely)
        val synchronizedAtMillis = clock.millis()
        launchRepositoryOperation {
            synchronizeActive(records, synchronizedAtMillis)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        persistPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val removedAtMillis = clock.millis()
        launchRepositoryOperation {
            markRemoved(sbn.key, removedAtMillis)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun persistPosted(sbn: StatusBarNotification) {
        val record = createRecordSafely(sbn) ?: return
        launchRepositoryOperation {
            upsert(record)
        }
    }

    private fun createRecordSafely(sbn: StatusBarNotification): NotificationRecord? =
        runCatching { recordFactory.create(sbn) }.getOrNull()

    private fun launchRepositoryOperation(
        operation: suspend NotificationRepository.() -> Unit
    ) {
        serviceScope.launch {
            try {
                repository.operation()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Notification contents are deliberately not logged.
            }
        }
    }
}
