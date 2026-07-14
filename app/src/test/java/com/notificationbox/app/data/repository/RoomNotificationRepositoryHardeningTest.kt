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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomNotificationRepositoryHardeningTest {
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
        repository = RoomNotificationRepository(database, clock)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `mark removed also prunes previously expired rows`() = runTest {
        repository.upsert(record("current"))
        database.notificationDao().upsert(expiredEntity("expired"))

        repository.markRemoved("current", nowMillis)

        assertNull(database.notificationDao().getByKey("expired"))
    }

    @Test
    fun `explicit prune removes expired rows without receiving a new notification`() = runTest {
        database.notificationDao().upsert(expiredEntity("expired"))

        repository.pruneExpired()

        assertNull(database.notificationDao().getByKey("expired"))
    }

    @Test
    fun `reset classification statistics clears aggregate decision and application keys`() = runTest {
        repository.upsert(record("one"))
        repository.setNotificationDecision("one", NotificationDecision.Ignore)
        repository.setAppRule(
            packageName = "com.example.app",
            appLabel = "Example",
            decision = NotificationDecision.HoldForDigest
        )

        repository.resetClassificationStats()

        assertEquals(
            0L,
            repository.observeClassificationStats().first().automaticallyClassified
        )
        assertEquals(
            0L,
            repository.observeClassificationStats().first().userOverrideChanges
        )
        assertEquals(
            0L,
            repository.observeClassificationStats().first().appRuleChanges
        )
        assertEquals(
            emptyMap<String, Long>(),
            repository.observeClassificationStats().first().appChangeCounts
        )
    }

    private fun record(key: String) = NotificationRecord(
        key = key,
        packageName = "com.example.app",
        appLabel = "Example",
        title = "title",
        text = "text",
        postTimeMillis = nowMillis,
        notificationId = 1,
        tag = null,
        channelId = "channel",
        category = NotificationDecision.KeepNow,
        reason = "test"
    )

    private fun expiredEntity(key: String): NotificationEntity {
        val expired = nowMillis - RoomNotificationRepository.RETENTION.toMillis() - 1
        return NotificationEntity(
            key = key,
            packageName = "com.example.app",
            appLabel = "Example",
            title = "title",
            text = "text",
            postTimeMillis = expired,
            notificationId = 1,
            tag = null,
            channelId = "channel",
            category = NotificationDecision.KeepNow.name,
            reason = "test",
            userDecision = null,
            userPinned = false,
            isActive = false,
            removedAtMillis = expired
        )
    }
}
