package com.notificationbox.app.ui

import android.content.Context
import com.notificationbox.app.R
import com.notificationbox.app.model.NotificationContentAvailability
import com.notificationbox.app.model.NotificationItem

fun interface NotificationContentPresenter {
    fun present(item: NotificationItem): NotificationItem

    companion object {
        val Identity = NotificationContentPresenter { item -> item }
    }
}

class AndroidNotificationContentPresenter(
    context: Context
) : NotificationContentPresenter {
    private val resources = context.applicationContext.resources

    override fun present(item: NotificationItem): NotificationItem =
        when (item.contentAvailability) {
            NotificationContentAvailability.AVAILABLE -> item
            NotificationContentAvailability.EMPTY -> item.copy(
                title = item.title
                    ?: resources.getString(R.string.notification_content_empty_title),
                text = null
            )
            NotificationContentAvailability.REDACTED_OR_UNAVAILABLE -> item.copy(
                title = resources.getString(R.string.notification_content_unavailable_title),
                text = resources.getString(R.string.notification_content_unavailable_body)
            )
        }
}
