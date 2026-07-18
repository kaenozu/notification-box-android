/*
 * File: app/src/test/java/com/notificationbox/app/data/db/NotificationSummaryDaoTest.kt
 * Description: Robolectric coverage for Room notification-summary aggregation and Flow updates.
 * Related: NotificationDao.kt, NotificationSummaryRow.kt, NotificationEntity.kt
 */
package com.notificationbox.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class NotificationSummaryDaoTest {
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
    fun `empty database emits zero counts`() = runTest {
        val summary = dao.observeSummarySince(sinceMillis = 0).first()

        assertEquals(0, summary.totalCount)
        assertEquals(0, summary.keepNowCount)
        assertEquals(0, summary.holdForDigestCount)
        assertEquals(0, summary.ignoreCount)
    }

    @Test
    fun `mixed automatic decisions emit correct counts`() = runTest {
        dao.upsert(entity("keep", 1_000, "KeepNow", userDecision = "Ignore"))
        dao.upsert(entity("digest-a", 1_100, "HoldForDigest"))
        dao.upsert(entity("digest-b", 1_200, "HoldForDigest"))
        dao.upsert(entity("ignore", 1_300, "Ignore", userDecision = "KeepNow"))

        val summary = dao.observeSummarySince(sinceMillis = 1_000).first()

        assertEquals(4, summary.totalCount)
        assertEquals(1, summary.keepNowCount)
        assertEquals(2, summary.holdForDigestCount)
        assertEquals(1, summary.ignoreCount)
    }

    @Test
    fun `time boundary includes notifications posted exactly at cutoff`() = runTest {
        dao.upsert(entity("before", 999, "KeepNow"))
        dao.upsert(entity("boundary", 1_000, "HoldForDigest"))
        dao.upsert(entity("after", 1_001, "Ignore"))

        val summary = dao.observeSummarySince(sinceMillis = 1_000).first()

        assertEquals(2, summary.totalCount)
        assertEquals(0, summary.keepNowCount)
        assertEquals(1, summary.holdForDigestCount)
        assertEquals(1, summary.ignoreCount)
    }

    @Test
    fun `summary Flow reflects inserts after it is created`() = runTest {
        val summaryFlow = dao.observeSummarySince(sinceMillis = 1_000)

        assertEquals(0, summaryFlow.first().totalCount)

        dao.upsert(entity("new", 1_000, "KeepNow"))
        val updated = summaryFlow.first()

        assertEquals(1, updated.totalCount)
        assertEquals(1, updated.keepNowCount)
    }

    private fun entity(
        key: String,
        postTimeMillis: Long,
        category: String,
        userDecision: String? = null
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
        userDecision = userDecision,
        userPinned = false,
        isActive = true,
        removedAtMillis = null
    )
}
