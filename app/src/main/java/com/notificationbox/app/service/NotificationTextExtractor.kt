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

    @Suppress("DEPRECATION")
    private fun extractMessagingStyle(notification: Notification): ExtractedNotificationText? =
        runCatching {
            val extras = notification.extras
            val messages: List<Notification.MessagingStyle.Message> =
                Notification.MessagingStyle.Message.getMessagesFromBundleArray(
                    extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                )
            val latestMessage: Notification.MessagingStyle.Message = messages
                .asReversed()
                .firstOrNull { message -> !message.text.isNullOrBlank() }
                ?: return@runCatching null
            val title = extras
                .getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
                ?.toString()
                ?: latestMessage.senderPerson?.name?.toString()
                ?: latestMessage.sender?.toString()

            ExtractedNotificationText(
                title = title?.takeIf(String::isNotBlank),
                text = latestMessage.text
                    ?.toString()
                    ?.takeIf(String::isNotBlank)
            )
        }.getOrNull()
}
