package com.notificationbox.app.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notificationbox.app.App
import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.model.IngestionErrorCode
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationRelayService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clock: Clock = Clock.systemUTC()
    private val reconciliationRequested = AtomicBoolean(false)
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

    override fun onCreate() {
        super.onCreate()
        val reporter = NotificationIngestionHealthStore
        commandQueue = NotificationCommandQueue(
            scope = serviceScope,
            processor = NotificationCommandProcessor(repository, reporter),
            healthReporter = reporter,
            onOverflow = ::requestSnapshotReconciliation
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        reconciliationRequested.set(false)
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

    private fun requestSnapshotReconciliation() {
        if (!reconciliationRequested.compareAndSet(false, true)) return

        serviceScope.launch {
            runCatching {
                requestUnbind()
                delay(REBIND_DELAY_MILLIS)
                requestRebind(
                    ComponentName(
                        this@NotificationRelayService,
                        NotificationRelayService::class.java
                    )
                )
            }.onFailure {
                reconciliationRequested.set(false)
                NotificationIngestionHealthStore.recordFailure(
                    IngestionErrorCode.RECONCILIATION_REQUEST_FAILED
                )
            }
        }
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
    }
}
