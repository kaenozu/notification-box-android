package com.notificationbox.app.service

import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.notificationbox.app.App
import com.notificationbox.app.domain.NotificationClassifier
import com.notificationbox.app.domain.NotificationRecord
import com.notificationbox.app.domain.NotificationSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationRelayService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val classifier = NotificationClassifier()
    private val appLabelCache = mutableMapOf<String, String>()

    private val selfPackageName: String by lazy { packageName }

    override fun onListenerConnected() {
        try {
            val active = activeNotifications ?: return
            serviceScope.launch {
                val repository = getRepository()
                val classifier = NotificationClassifier()
                for (sbn in active) {
                    if (sbn.packageName == packageName) continue
                    val record = buildRecord(sbn, classifier)
                    if (record != null) {
                        repository.upsert(record)
                    }
                }
            }
        } catch (_: SecurityException) {
            Log.w(TAG, "SecurityException accessing activeNotifications")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == selfPackageName) return
        serviceScope.launch {
            val repository = getRepository()
            val record = buildRecord(sbn, classifier) ?: return@launch
            repository.upsert(record)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == selfPackageName) return
        serviceScope.launch {
            val repository = getRepository()
            repository.markRemoved(sbn.key, System.currentTimeMillis())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun getRepository() =
        (application as App).container.notificationRepository

    private fun buildRecord(sbn: StatusBarNotification, clf: NotificationClassifier): NotificationRecord? {
        val extras = sbn.notification.extras
        val key = sbn.key ?: return null
        val packageName = sbn.packageName
        val appLabel = getAppLabel(packageName)
        val title = safeString(extras, Notification.EXTRA_TITLE)
            ?: safeString(extras, Notification.EXTRA_TITLE_BIG)
        val text = safeString(extras, Notification.EXTRA_BIG_TEXT)
            ?: safeString(extras, Notification.EXTRA_TEXT)
            ?: safeTextLines(extras)
        val postTimeMillis = sbn.postTime
        val notificationId = sbn.id
        val tag = sbn.tag
        val channelId = sbn.notification.channelId

        val sample = NotificationSample(packageName, title, text)
        val (decision, reason) = clf.classify(sample)

        return NotificationRecord(
            key = key,
            packageName = packageName,
            appLabel = appLabel,
            title = truncate(title, 512),
            text = truncate(text, 2048),
            postTimeMillis = postTimeMillis,
            notificationId = notificationId,
            tag = tag,
            channelId = channelId,
            category = decision,
            reason = reason,
            userPinned = false,
            isActive = true,
            removedAtMillis = null
        )
    }

    private fun getAppLabel(packageName: String): String {
        appLabelCache[packageName]?.let { return it }
        val label = try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai)?.toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val result = label ?: packageName.substringAfterLast('.').ifEmpty { packageName }
        appLabelCache[packageName] = result
        return result
    }

    private fun safeString(extras: android.os.Bundle, key: String): String? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val cs = extras.getCharSequence(key) ?: return null
            return cs.toString().takeIf { it.isNotBlank() }
        }
        @Suppress("DEPRECATION")
        val value = extras.get(key) ?: return null
        return when (value) {
            is CharSequence -> value.toString().takeIf { it.isNotBlank() }
            is Array<*> -> null
            else -> value.toString().takeIf { it.isNotBlank() }
        }
    }

    private fun safeTextLines(extras: android.os.Bundle): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return null
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) ?: return null
        if (lines.isEmpty()) return null
        val joined = lines.joinToString("\n") { it?.toString().orEmpty() }
        return joined.takeIf { it.isNotBlank() }
    }

    private fun truncate(value: String?, max: Int): String? {
        if (value == null) return null
        return if (value.length > max) value.take(max) else value
    }

    companion object {
        private const val TAG = "NotificationRelay"
    }
}
