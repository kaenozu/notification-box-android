package com.notificationbox.app.service

import android.service.notification.StatusBarNotification
import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.domain.NotificationClassifier
import com.notificationbox.app.domain.NotificationSample

class NotificationRecordFactory(
    private val ownPackageName: String,
    private val appLabelResolver: AppLabelResolver,
    private val classifier: NotificationClassifier = NotificationClassifier()
) {
    fun create(sbn: StatusBarNotification): NotificationRecord? {
        if (sbn.packageName == ownPackageName) return null

        val extracted = NotificationTextExtractor.extract(sbn.notification)
        val sample = NotificationSample(
            packageName = sbn.packageName,
            title = extracted.title,
            text = extracted.text
        )
        val (decision, reason) = classifier.classify(sample)

        return NotificationRecord(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = appLabelResolver.resolve(sbn.packageName),
            title = extracted.title,
            text = extracted.text,
            postTimeMillis = sbn.postTime,
            notificationId = sbn.id,
            tag = sbn.tag,
            channelId = sbn.notification.channelId,
            category = decision,
            reason = reason,
            isActive = true,
            removedAtMillis = null
        )
    }
}
