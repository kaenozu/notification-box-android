package com.notificationbox.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notificationbox.app.App
import com.notificationbox.app.data.repository.NotificationRepository
import java.time.Clock
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
        }.getOrElse { emptyList() }

        currentNotifications.forEach(::persistPosted)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        persistPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        serviceScope.launch {
            repository.markRemoved(sbn.key, clock.millis())
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun persistPosted(sbn: StatusBarNotification) {
        val record = recordFactory.create(sbn) ?: return
        serviceScope.launch {
            repository.upsert(record)
        }
    }
}
