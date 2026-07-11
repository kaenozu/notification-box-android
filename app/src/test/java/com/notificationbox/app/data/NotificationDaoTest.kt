package com.notificationbox.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationDaoTest {

    private lateinit var db: NotificationDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotificationDatabase::class.java
        ).build()
        dao = db.dao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun createEntity(key: String = "key-1", postTime: Long = 1000L) = NotificationEntity(
        key = key,
        packageName = "com.test.app",
        appLabel = "Test",
        title = "title",
        text = "text",
        postTimeMillis = postTime,
        notificationId = 1,
        tag = null,
        channelId = "ch1",
        category = "KeepNow",
        reason = "test",
        userPinned = false,
        isActive = true,
        removedAtMillis = null
    )

    @Test
    fun `same key upsert updates content`() = runTest {
        dao.upsert(createEntity("k1", 1000L))
        dao.upsert(createEntity("k1", 2000L))
        val list = dao.observeAll().first()
        assertEquals(1, list.size)
        assertEquals(2000L, list.first().postTimeMillis)
    }

    @Test
    fun `different keys create separate rows`() = runTest {
        dao.upsert(createEntity("k1", 1000L))
        dao.upsert(createEntity("k2", 2000L))
        val list = dao.observeAll().first()
        assertEquals(2, list.size)
    }

    @Test
    fun `list ordered by postTimeMillis descending`() = runTest {
        dao.upsert(createEntity("k1", 1000L))
        dao.upsert(createEntity("k2", 2000L))
        val list = dao.observeAll().first()
        assertEquals("k2", list.first().key)
    }

    @Test
    fun `setPinned updates pin status`() = runTest {
        dao.upsert(createEntity("k1"))
        dao.setPinned("k1", true)
        val entity = dao.getByKey("k1")
        assertTrue(entity?.userPinned == true)
    }

    @Test
    fun `markRemoved sets inactive`() = runTest {
        dao.upsert(createEntity("k1"))
        dao.markRemoved("k1", 5000L)
        val entity = dao.getByKey("k1")
        assertTrue(entity?.isActive == false)
        assertEquals(5000L, entity?.removedAtMillis)
    }

    @Test
    fun `deleteByKey removes entity`() = runTest {
        dao.upsert(createEntity("k1"))
        dao.deleteByKey("k1")
        assertNull(dao.getByKey("k1"))
    }

    @Test
    fun `deleteAll removes all`() = runTest {
        dao.upsert(createEntity("k1"))
        dao.upsert(createEntity("k2"))
        dao.deleteAll()
        val list = dao.observeAll().first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `deleteOldUnpinned removes old notifications`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsert(createEntity("old", now - 10 * 86_400_000L))
        dao.upsert(createEntity("recent", now))
        val deleted = dao.deleteOldUnpinned(now - 7 * 86_400_000L)
        assertEquals(1, deleted)
    }

    @Test
    fun `pinned notifications survive pruning`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsert(createEntity("pinned-old", now - 30 * 86_400_000L).copy(userPinned = true))
        val deleted = dao.deleteOldUnpinned(now - 7 * 86_400_000L)
        assertEquals(0, deleted)
    }

    @Test
    fun `deleteOldestUnpinned removes excess`() = runTest {
        for (i in 1..5) {
            dao.upsert(createEntity("k$i", i * 1000L))
        }
        val deleted = dao.deleteOldestUnpinned(3)
        assertEquals(3, deleted)
        val remaining = dao.observeAll().first()
        assertEquals(2, remaining.size)
    }
}
