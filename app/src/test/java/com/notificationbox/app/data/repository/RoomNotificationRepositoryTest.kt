package com.notificationbox.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.notificationbox.app.data.db.NotificationDatabase
import com.notificationbox.app.data.db.NotificationEntity
import com.notificationbox.app.model.NotificationDecision
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomNotificationRepositoryTest {
    private val nowMillis = 10_000_000_000L
    private val clock = Clock.fixed(Instant.ofEpochMilli(nowMillis), ZoneOffset.UTC)

    private lateinit var database: NotificationDatabase
    private lateinit var repository: RoomNotificationRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomNotificationRepository(database.notificationDao(), clock)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `reposting same key preserves pin state`() = runTest {
        repository.upsert(record(key = "same", title = "before"))
        repository.setPinned("same", true)

        repository.upsert(record(key = "same", title = "after"))

        val item = repository.observeNotifications().first().single()
        assertEquals("after", item.title)
        assertTrue(item.userPinned)
    }

    @Test
    fun `upsert prunes expired inactive unpinned rows but keeps pinned and active rows`() = runTest {
        val expiredTime = nowMillis - RoomNotificationRepository.RETENTION.toMillis() - 1
        database.notificationDao().upsert(
            entity(key = "expired", postTimeMillis = expiredTime, isActive = false)
        )
        database.notificationDao().upsert(
            entity(key = "pinned", postTimeMillis = expiredTime, userPinned = true, isActive = false)
        )
        database.notificationDao().upsert(
            entity(key = "active", postTimeMillis = expiredTime, isActive = true)
        )

        repository.upsert(record(key = "new"))

        assertNull(database.notificationDao().getByKey("expired"))
        assertTrue(database.notificationDao().getByKey("pinned") != null)
        assertTrue(database.notificationDao().getByKey("active") != null)
        assertTrue(database.notificationDao().getByKey("new") != null)
    }

    @Test
    fun `upsert enforces maximum count by deleting oldest unpinned row`() = runTest {
        repeat(RoomNotificationRepository.MAX_NOTIFICATION_COUNT) { index ->
            database.notificationDao().upsert(
                entity(key = "key-$index", postTimeMillis = index.toLong())
            )
        }

        repository.upsert(record(key = "new", postTimeMillis = nowMillis))

        assertEquals(RoomNotificationRepository.MAX_NOTIFICATION_COUNT, database.notificationDao().count())
        assertNull(database.notificationDao().getByKey("key-0"))
        assertTrue(database.notificationDao().getByKey("new") != null)
    }

    @Test
    fun `active synchronization marks missing notifications removed and refreshes present ones`() = runTest {
        repository.upsert(record(key = "missing", title = "old"))
        repository.upsert(record(key = "present", title = "before"))

        repository.synchronizeActive(
            notifications = listOf(record(key = "present", title = "after")),
            synchronizedAtMillis = nowMillis
        )

        val missing = database.notificationDao().getByKey("missing")
        val present = database.notificationDao().getByKey("present")
        assertFalse(missing?.isActive ?: true)
        assertEquals(nowMillis, missing?.removedAtMillis)
        assertTrue(present?.isActive == true)
        assertNull(present?.removedAtMillis)
        assertEquals("after", present?.title)
    }

    @Test
    fun `empty active synchronization marks every active row removed`() = runTest {
        repository.upsert(record(key = "one"))
        repository.upsert(record(key = "two"))

        repository.synchronizeActive(emptyList(), synchronizedAtMillis = nowMillis)

        assertTrue(database.notificationDao().getAllOnce().all { !it.isActive })
    }

    @Test
    fun `unknown stored category falls back safely`() = runTest {
        database.notificationDao().upsert(entity(key = "unknown", category = "FutureValue"))

        val item = repository.observeNotifications().first().single()

        assertEquals(NotificationDecision.HoldForDigest, item.category)
    }

    private fun record(
        key: String,
        title: String? = "title",
        postTimeMillis: Long = nowMillis
    ): NotificationRecord =
        NotificationRecord(
            key = key,
            packageName = "com.example.app",
            appLabel = "Example",
            title = title,
            text = "text",
            postTimeMillis = postTimeMillis,
            notificationId = 1,
            tag = null,
            channelId = "channel",
            category = NotificationDecision.HoldForDigest,
            reason = "test"
        )

    private fun entity(
        key: String,
        postTimeMillis: Long = nowMillis,
        userPinned: Boolean = false,
        category: String = NotificationDecision.HoldForDigest.name,
        isActive: Boolean = true
    ): NotificationEntity =
        NotificationEntity(
            key = key,
            packageName = "com.example.app",
            appLabel = "Example",
            title = "title",
            text = "text",
            postTimeMillis = postTimeMillis,
            notificationId = 1,
            tag = null,
            channelId = "channel",
            category = category,
            reason = "test",
            userPinned = userPinned,
            isActive = isActive,
            removedAtMillis = if (isActive) null else postTimeMillis
        )
}
