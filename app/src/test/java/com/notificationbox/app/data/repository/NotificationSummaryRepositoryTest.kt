/*
 * File: app/src/test/java/com/notificationbox/app/data/repository/NotificationSummaryRepositoryTest.kt
 * Description: Verifies Room aggregate rows are mapped to the summary domain model.
 * Related: RoomNotificationRepository.kt, NotificationDao.kt, NotificationSummary.kt
 */
package com.notificationbox.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.notificationbox.app.data.db.NotificationDatabase
import com.notificationbox.app.data.db.NotificationEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationSummaryRepositoryTest {
    private lateinit var database: NotificationDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `Room summary row maps counts and timestamps to domain model`() = runTest {
        val now = Instant.parse("2026-07-18T00:00:00Z")
        val since = Instant.parse("2026-07-17T00:00:00Z")
        database.notificationDao().upsert(entity("keep", since.toEpochMilli(), "KeepNow"))
        database.notificationDao().upsert(
            entity("digest", since.plusSeconds(1).toEpochMilli(), "HoldForDigest")
        )
        database.notificationDao().upsert(
            entity("ignore", since.plusSeconds(2).toEpochMilli(), "Ignore")
        )
        database.notificationDao().upsert(
            entity("old", since.minusMillis(1).toEpochMilli(), "KeepNow")
        )
        val repository = RoomNotificationRepository(
            database = database,
            clock = Clock.fixed(now, ZoneOffset.UTC)
        )

        val summary = repository.observeSummarySince(since).first()

        assertEquals(3, summary.totalCount)
        assertEquals(1, summary.keepNowCount)
        assertEquals(1, summary.holdForDigestCount)
        assertEquals(1, summary.ignoreCount)
        assertEquals(since, summary.periodStart)
        assertEquals(now, summary.generatedAt)
    }

    @Test
    fun `repository emits zero summary for an empty database`() = runTest {
        val now = Instant.parse("2026-07-18T00:00:00Z")
        val since = now.minusSeconds(24 * 60 * 60)
        val repository = RoomNotificationRepository(
            database = database,
            clock = Clock.fixed(now, ZoneOffset.UTC)
        )

        val summary = repository.observeSummarySince(since).first()

        assertEquals(0, summary.totalCount)
        assertEquals(0, summary.keepNowCount)
        assertEquals(0, summary.holdForDigestCount)
        assertEquals(0, summary.ignoreCount)
        assertEquals(since, summary.periodStart)
        assertEquals(now, summary.generatedAt)
    }

    private fun entity(
        key: String,
        postTimeMillis: Long,
        category: String
    ) = NotificationEntity(
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
        userDecision = null,
        userPinned = false,
        isActive = true,
        removedAtMillis = null
    )
}
