package com.notificationbox.app.service

import android.service.notification.StatusBarNotification
import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.domain.NotificationClassifier
import com.notificationbox.app.domain.NotificationSample
import com.notificationbox.app.model.NotificationContentAvailability
import com.notificationbox.app.model.NotificationDecision

class NotificationRecordFactory(
    private val ownPackageName: String,
    private val appLabelResolver: AppLabelResolver,
    private val classifier: NotificationClassifier = NotificationClassifier()
) {
    fun create(sbn: StatusBarNotification): NotificationRecord? {
        if (sbn.packageName == ownPackageName) return null

        val extracted = NotificationTextExtractor.extract(sbn.notification)
        val (decision, reason) = when (extracted.availability) {
            NotificationContentAvailability.AVAILABLE -> classifier.classify(
                NotificationSample(
                    packageName = sbn.packageName,
                    title = extracted.title,
                    text = extracted.text
                )
            )

            NotificationContentAvailability.EMPTY ->
                NotificationDecision.HoldForDigest to
                    "通知内容が空のため安全側で「あとで確認」に分類"

            NotificationContentAvailability.REDACTED_OR_UNAVAILABLE ->
                NotificationDecision.HoldForDigest to
                    "通知内容を取得できないため安全側で「あとで確認」に分類"
        }

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
            contentAvailability = extracted.availability,
            isActive = true,
            removedAtMillis = null
        )
    }
}
