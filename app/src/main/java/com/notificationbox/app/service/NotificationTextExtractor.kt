package com.notificationbox.app.service

import android.app.Notification

data class ExtractedNotificationText(
    val title: String?,
    val text: String?
)

object NotificationTextExtractor {
    fun extract(notification: Notification): ExtractedNotificationText {
        val extras = notification.extras
        val messaging = extractMessagingStyle(notification)

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: messaging?.title

        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: messaging?.text
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.joinToString(separator = "\n") { it.toString() }
                ?.takeIf(String::isNotBlank)

        return ExtractedNotificationText(
            title = title?.takeIf(String::isNotBlank),
            text = text?.takeIf(String::isNotBlank)
        )
    }

    private fun extractMessagingStyle(notification: Notification): ExtractedNotificationText? =
        runCatching {
            val style = Notification.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
                ?: return@runCatching null
            val latestMessage = style.messages
                .asReversed()
                .firstOrNull { message -> !message.text.isNullOrBlank() }
            val title = style.conversationTitle?.toString()
                ?: latestMessage?.senderPerson?.name?.toString()
            ExtractedNotificationText(
                title = title?.takeIf(String::isNotBlank),
                text = latestMessage?.text?.toString()?.takeIf(String::isNotBlank)
            )
        }.getOrNull()
}
