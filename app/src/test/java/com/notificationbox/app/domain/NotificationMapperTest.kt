package com.notificationbox.app.domain

import com.notificationbox.app.data.NotificationEntity
import com.notificationbox.app.model.NotificationDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMapperTest {

    @Test
    fun `entity to record maps all fields`() {
        val entity = NotificationEntity(
            key = "test-key",
            packageName = "com.test",
            appLabel = "TestApp",
            title = "title",
            text = "text",
            postTimeMillis = 1000L,
            notificationId = 42,
            tag = "tag1",
            channelId = "ch1",
            category = "KeepNow",
            reason = "test",
            userPinned = true,
            isActive = true,
            removedAtMillis = null
        )
        val record = entity.toRecord()
        assertEquals("test-key", record.key)
        assertEquals("com.test", record.packageName)
        assertEquals("TestApp", record.appLabel)
        assertEquals("title", record.title)
        assertEquals("text", record.text)
        assertEquals(1000L, record.postTimeMillis)
        assertEquals(42, record.notificationId)
        assertEquals("tag1", record.tag)
        assertEquals("ch1", record.channelId)
        assertEquals(NotificationDecision.KeepNow, record.category)
        assertEquals("test", record.reason)
        assertTrue(record.userPinned)
        assertTrue(record.isActive)
    }

    @Test
    fun `parseCategory invalid returns HoldForDigest`() {
        assertEquals(NotificationDecision.HoldForDigest, parseCategory("invalid"))
    }

    @Test
    fun `parseCategory null returns HoldForDigest`() {
        assertEquals(NotificationDecision.HoldForDigest, parseCategory(null))
    }

    @Test
    fun `parseCategory valid returns correct enum`() {
        assertEquals(NotificationDecision.KeepNow, parseCategory("KeepNow"))
        assertEquals(NotificationDecision.HoldForDigest, parseCategory("HoldForDigest"))
        assertEquals(NotificationDecision.Ignore, parseCategory("Ignore"))
    }

    @Test
    fun `record to entity and back is identity`() {
        val record = NotificationRecord(
            key = "key",
            packageName = "com.test",
            appLabel = "App",
            title = "t", text = "b",
            postTimeMillis = 100L,
            notificationId = 1, tag = null, channelId = "ch",
            category = NotificationDecision.KeepNow,
            reason = "r", userPinned = false, isActive = true, removedAtMillis = null
        )
        val entity = record.toEntity()
        assertEquals(record.key, entity.key)
        assertEquals(record.category.name, entity.category)
        val back = entity.toRecord()
        assertEquals(record.key, back.key)
        assertEquals(record.category, back.category)
    }
}
