package com.notificationbox.app.service

import android.app.Notification

data class ExtractedNotificationText(
    val title: String?,
    val text: String?
)

object NotificationTextExtractor {
    fun extract(notification: Notification): ExtractedNotificationText {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()

        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.joinToString(separator = "\n") { it.toString() }
                ?.takeIf(String::isNotBlank)

        return ExtractedNotificationText(
            title = title?.takeIf(String::isNotBlank),
            text = text?.takeIf(String::isNotBlank)
        )
    }
}
