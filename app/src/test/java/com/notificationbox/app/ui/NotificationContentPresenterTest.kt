package com.notificationbox.app.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.notificationbox.app.R
import com.notificationbox.app.model.DecisionSource
import com.notificationbox.app.model.NotificationContentAvailability
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationContentPresenterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val presenter = AndroidNotificationContentPresenter(context)

    @Test
    fun `available content remains unchanged`() {
        val item = item(
            title = "Title",
            text = "Body",
            availability = NotificationContentAvailability.AVAILABLE
        )

        assertEquals(item, presenter.present(item))
    }

    @Test
    fun `empty content uses localized empty title`() {
        val presented = presenter.present(
            item(
                title = null,
                text = null,
                availability = NotificationContentAvailability.EMPTY
            )
        )

        assertEquals(
            context.getString(R.string.notification_content_empty_title),
            presented.title
        )
        assertNull(presented.text)
    }

    @Test
    fun `unavailable content uses localized explanation`() {
        val presented = presenter.present(
            item(
                title = null,
                text = null,
                availability = NotificationContentAvailability.REDACTED_OR_UNAVAILABLE
            )
        )

        assertEquals(
            context.getString(R.string.notification_content_unavailable_title),
            presented.title
        )
        assertEquals(
            context.getString(R.string.notification_content_unavailable_body),
            presented.text
        )
    }

    private fun item(
        title: String?,
        text: String?,
        availability: NotificationContentAvailability
    ) = NotificationItem(
        key = "key",
        packageName = "com.example",
        appLabel = "Example",
        title = title,
        text = text,
        postTime = Instant.EPOCH,
        automaticDecision = NotificationDecision.HoldForDigest,
        userDecision = null,
        appRuleDecision = null,
        category = NotificationDecision.HoldForDigest,
        decisionSource = DecisionSource.Automatic,
        automaticReason = "test",
        reason = "test",
        contentAvailability = availability
    )
}
