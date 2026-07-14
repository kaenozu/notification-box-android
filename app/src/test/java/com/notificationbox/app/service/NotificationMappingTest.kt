package com.notificationbox.app.service

import android.app.Notification
import android.app.Person
import android.content.Context
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationMappingTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `regular title has priority over big title`() {
        val notification = Notification().apply {
            extras.putCharSequence(Notification.EXTRA_TITLE, "regular title")
            extras.putCharSequence(Notification.EXTRA_TITLE_BIG, "big title")
        }

        val extracted = NotificationTextExtractor.extract(notification)

        assertEquals("regular title", extracted.title)
    }

    @Test
    fun `big title is used when regular title is absent`() {
        val notification = Notification().apply {
            extras.putCharSequence(Notification.EXTRA_TITLE_BIG, "big title")
        }

        val extracted = NotificationTextExtractor.extract(notification)

        assertEquals("big title", extracted.title)
    }

    @Test
    fun `big text has priority over regular text`() {
        val notification = Notification.Builder(context, "channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("title")
            .setContentText("short")
            .setStyle(Notification.BigTextStyle().bigText("expanded"))
            .build()

        val extracted = NotificationTextExtractor.extract(notification)

        assertEquals("title", extracted.title)
        assertEquals("expanded", extracted.text)
    }

    @Test
    fun `messaging style extracts latest structured message`() {
        val self = Person.Builder().setName("Me").build()
        val sender = Person.Builder().setName("Alice").build()
        val notification = Notification.Builder(context, "channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setStyle(
                Notification.MessagingStyle(self)
                    .setConversationTitle("Project")
                    .addMessage("older", 1L, sender)
                    .addMessage("latest", 2L, sender)
            )
            .build()

        val extracted = NotificationTextExtractor.extract(notification)

        assertEquals("Project", extracted.title)
        assertEquals("latest", extracted.text)
    }

    @Test
    fun `messaging style falls back to latest sender when conversation title is absent`() {
        val self = Person.Builder().setName("Me").build()
        val sender = Person.Builder().setName("Alice").build()
        val notification = Notification.Builder(context, "channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setStyle(
                Notification.MessagingStyle(self)
                    .addMessage("latest", 2L, sender)
            )
            .build()

        val extracted = NotificationTextExtractor.extract(notification)

        assertEquals("Alice", extracted.title)
        assertEquals("latest", extracted.text)
    }

    @Test
    fun `regular text is used when big and messaging text are absent`() {
        val notification = Notification().apply {
            extras.putCharSequence(Notification.EXTRA_TEXT, "regular text")
        }

        val extracted = NotificationTextExtractor.extract(notification)

        assertEquals("regular text", extracted.text)
    }

    @Test
    fun `text lines are joined when no regular text exists`() {
        val notification = Notification().apply {
            extras.putCharSequenceArray(
                Notification.EXTRA_TEXT_LINES,
                arrayOf("line one", "line two")
            )
        }

        val extracted = NotificationTextExtractor.extract(notification)

        assertEquals("line one\nline two", extracted.text)
    }

    @Test
    fun `own package notifications are excluded`() {
        val factory = NotificationRecordFactory(
            ownPackageName = "com.notificationbox.app",
            appLabelResolver = AppLabelResolver { "Notification Box" }
        )

        val record = factory.create(
            statusBarNotification("com.notificationbox.app")
        )

        assertNull(record)
    }

    @Test
    fun `status bar key and metadata are preserved`() {
        val factory = NotificationRecordFactory(
            ownPackageName = "com.notificationbox.app",
            appLabelResolver = AppLabelResolver { "Example" }
        )
        val sbn = statusBarNotification(
            "com.example.app",
            id = 42,
            tag = "tag",
            postTime = 1234
        )

        val record = requireNotNull(factory.create(sbn))

        assertEquals(sbn.key, record.key)
        assertEquals(42, record.notificationId)
        assertEquals("tag", record.tag)
        assertEquals(1234, record.postTimeMillis)
        assertTrue(record.isActive)
    }

    @Test
    fun `application label resolver falls back to package suffix`() {
        val resolver = CachingAppLabelResolver(context.packageManager)

        assertEquals("missing", resolver.resolve("not.installed.missing"))
    }

    @Suppress("DEPRECATION")
    private fun statusBarNotification(
        packageName: String,
        id: Int = 1,
        tag: String? = null,
        postTime: Long = 1000
    ): StatusBarNotification {
        val notification = Notification.Builder(context, "channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("title")
            .setContentText("text")
            .build()
        return StatusBarNotification(
            packageName,
            packageName,
            id,
            tag,
            1000,
            1000,
            0,
            notification,
            Process.myUserHandle(),
            postTime
        )
    }
}
