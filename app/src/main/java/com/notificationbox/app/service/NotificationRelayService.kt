package com.notificationbox.app.service

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notificationbox.app.App
import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.model.IngestionErrorCode
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationRelayService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clock: Clock = Clock.systemUTC()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var commandQueue: NotificationCommandQueue

    private val repository: NotificationRepository by lazy {
        (application as App).container.notificationRepository
    }

    private val recordFactory: NotificationRecordFactory by lazy {
        NotificationRecordFactory(
            ownPackageName = packageName,
            appLabelResolver = CachingAppLabelResolver(packageManager)
        )
    }

    private val rebindCoordinator: NotificationRebindCoordinator by lazy {
        val componentName = ComponentName(
            applicationContext,
            NotificationRelayService::class.java
        )
        NotificationRebindCoordinator(
            schedule = { runnable, delayMillis ->
                mainHandler.postDelayed(runnable, delayMillis)
            },
            cancelScheduled = mainHandler::removeCallbacks,
            requestUnbind = ::requestUnbind,
            requestRebind = { requestRebind(componentName) },
            recordFailure = {
                NotificationIngestionHealthStore.recordFailure(
                    IngestionErrorCode.ACTIVE_SNAPSHOT_FAILED
                )
            },
            recordTimeout = {
                NotificationIngestionHealthStore.recordFailure(
                    IngestionErrorCode.REBIND_TIMEOUT
                )
            },
            delayMillis = REBIND_DELAY_MILLIS,
            connectionTimeoutMillis = REBIND_CONNECTION_TIMEOUT_MILLIS,
            maxAttempts = REBIND_MAX_ATTEMPTS
        )
    }

    override fun onCreate() {
        super.onCreate()
        val reporter = NotificationIngestionHealthStore
        val container = (application as App).container
        commandQueue = NotificationCommandQueue(
            scope = serviceScope,
            processor = NotificationCommandProcessor(
                repository = repository,
                healthReporter = reporter,
                paymentSink = container.paymentNotificationSink
            ),
            healthReporter = reporter,
            onOverflow = rebindCoordinator::request
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        rebindCoordinator.markConnected()
        synchronizeCurrentNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val record = createRecordSafely(sbn) ?: return
        commandQueue.submit(NotificationCommand.Upsert(record))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        commandQueue.submit(
            NotificationCommand.MarkRemoved(
                key = sbn.key,
                removedAtMillis = clock.millis()
            )
        )
    }

    override fun onDestroy() {
        rebindCoordinator.cancel()
        if (::commandQueue.isInitialized) {
            commandQueue.close()
            serviceScope.launch {
                commandQueue.join()
                serviceScope.cancel()
            }
        } else {
            serviceScope.cancel()
        }
        super.onDestroy()
    }

    private fun synchronizeCurrentNotifications() {
        val currentNotifications = runCatching {
            activeNotifications?.toList().orEmpty()
        }.getOrElse {
            NotificationIngestionHealthStore.recordFailure(
                IngestionErrorCode.ACTIVE_SNAPSHOT_FAILED
            )
            return
        }

        val activeKeys = currentNotifications
            .asSequence()
            .filterNot { it.packageName == packageName }
            .map(StatusBarNotification::getKey)
            .toSet()
        val records = currentNotifications.mapNotNull(::createRecordSafely)
        commandQueue.submit(
            NotificationCommand.SynchronizeActive(
                activeKeys = activeKeys,
                notifications = records,
                synchronizedAtMillis = clock.millis()
            )
        )
    }

    private fun createRecordSafely(sbn: StatusBarNotification): NotificationRecord? {
        if (sbn.packageName == packageName) return null
        return try {
            recordFactory.create(sbn)
        } catch (_: Exception) {
            NotificationIngestionHealthStore.recordFailure(
                IngestionErrorCode.RECORD_MAPPING_FAILED
            )
            null
        }
    }

    companion object {
        private const val REBIND_DELAY_MILLIS = 250L
        private const val REBIND_CONNECTION_TIMEOUT_MILLIS = 5_000L
        private const val REBIND_MAX_ATTEMPTS = 3
    }
}
