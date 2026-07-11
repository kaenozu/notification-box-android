package com.notificationbox.app.service

import android.service.notification.StatusBarNotification
import android.service.notification.NotificationListenerService
import com.notificationbox.app.data.NotificationStore

class NotificationRelayService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        NotificationStore.upsertFromListener(
            packageName = sbn.packageName,
            title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        )
    }
}
