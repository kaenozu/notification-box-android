package com.notificationbox.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationDaoTest {
    private lateinit var database: NotificationDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.notificationDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `upsert with same key updates one row`() = runTest {
        dao.upsert(entity(key = "same", title = "before"))
        dao.upsert(entity(key = "same", title = "after"))

        assertEquals(1, dao.count())
        assertEquals("after", dao.getByKey("same")?.title)
    }

    @Test
    fun `different keys with same timestamp are both retained`() = runTest {
        dao.upsert(entity(key = "a", postTimeMillis = 100))
        dao.upsert(entity(key = "b", postTimeMillis = 100))

        assertEquals(2, dao.count())
    }

    @Test
    fun `notifications are ordered newest first`() = runTest {
        dao.upsert(entity(key = "old", postTimeMillis = 100))
        dao.upsert(entity(key = "new", postTimeMillis = 200))

        assertEquals(listOf("new", "old"), dao.observeAll().first().map { it.key })
    }

    @Test
    fun `manual decision can be set and cleared`() = runTest {
        dao.upsert(entity(key = "decision"))

        dao.setUserDecision("decision", "KeepNow")
        assertEquals("KeepNow", dao.getByKey("decision")?.userDecision)

        dao.setUserDecision("decision", null)
        assertNull(dao.getByKey("decision")?.userDecision)
    }

    @Test
    fun `pin state is persisted`() = runTest {
        dao.upsert(entity(key = "pinned"))

        dao.setPinned("pinned", true)

        assertTrue(dao.getByKey("pinned")?.userPinned == true)
    }

    @Test
    fun `mark removed keeps history and marks inactive`() = runTest {
        dao.upsert(entity(key = "removed"))

        dao.markRemoved("removed", 500)

        val stored = dao.getByKey("removed")
        assertFalse(stored?.isActive ?: true)
        assertEquals(500L, stored?.removedAtMillis)
    }

    @Test
    fun `clear all removes every notification`() = runTest {
        dao.upsert(entity(key = "a"))
        dao.upsert(entity(key = "b"))

        dao.clearAll()

        assertEquals(0, dao.count())
        assertNull(dao.getByKey("a"))
    }

    @Test
    fun `expired inactive unpinned notifications are deleted but pinned and active remain`() = runTest {
        dao.upsert(entity(key = "expired", postTimeMillis = 10, userPinned = false, isActive = false))
        dao.upsert(entity(key = "pinned", postTimeMillis = 10, userPinned = true, isActive = false))
        dao.upsert(entity(key = "active", postTimeMillis = 10, userPinned = false, isActive = true))
        dao.upsert(entity(key = "recent", postTimeMillis = 1_000, userPinned = false, isActive = false))

        dao.deleteExpired(cutoffMillis = 100)

        assertNull(dao.getByKey("expired"))
        assertTrue(dao.getByKey("pinned") != null)
        assertTrue(dao.getByKey("active") != null)
        assertTrue(dao.getByKey("recent") != null)
    }

    @Test
    fun `maximum count removes oldest unpinned rows`() = runTest {
        repeat(502) { index ->
            dao.upsert(entity(key = "key-$index", postTimeMillis = index.toLong()))
        }

        dao.pruneToMaximum(500)

        assertEquals(500, dao.count())
        assertNull(dao.getByKey("key-0"))
        assertNull(dao.getByKey("key-1"))
        assertTrue(dao.getByKey("key-501") != null)
    }

    @Test
    fun `maximum count prefers deleting inactive rows`() = runTest {
        repeat(499) { index ->
            dao.upsert(entity(key = "active-$index", postTimeMillis = index.toLong(), isActive = true))
        }
        dao.upsert(entity(key = "inactive", postTimeMillis = 10_000, isActive = false))
        dao.upsert(entity(key = "old-active", postTimeMillis = -1, isActive = true))

        dao.pruneToMaximum(500)

        assertNull(dao.getByKey("inactive"))
        assertTrue(dao.getByKey("old-active") != null)
    }

    private fun entity(
        key: String,
        title: String? = "title",
        postTimeMillis: Long = 1_000,
        userDecision: String? = null,
        userPinned: Boolean = false,
        isActive: Boolean = true
    ): NotificationEntity =
        NotificationEntity(
            key = key,
            packageName = "com.example.app",
            appLabel = "Example",
            title = title,
            text = "text",
            postTimeMillis = postTimeMillis,
            notificationId = 1,
            tag = null,
            channelId = "channel",
            category = "HoldForDigest",
            reason = "test",
            userDecision = userDecision,
            userPinned = userPinned,
            isActive = isActive,
            removedAtMillis = if (isActive) null else postTimeMillis
        )
}
