package com.notificationbox.app.service

import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.model.AppRule
import com.notificationbox.app.model.ClassificationStats
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationEventProcessorTest {
    @Test
    fun `posted then removed remains inactive even when upsert is delayed`() = runTest {
        val repository = RecordingRepository()
        val upsertStarted = CompletableDeferred<Unit>()
        val releaseUpsert = CompletableDeferred<Unit>()
        repository.beforeUpsert = {
            upsertStarted.complete(Unit)
            releaseUpsert.await()
        }
        val processor = processor(repository)

        assertTrue(processor.enqueue(NotificationRepositoryEvent.Posted(record("key"))))
        runCurrent()
        upsertStarted.await()
        assertTrue(processor.enqueue(NotificationRepositoryEvent.Removed("key", 2_000)))
        releaseUpsert.complete(Unit)
        advanceUntilIdle()
        processor.close()
        processor.join()

        assertEquals(listOf("posted:key", "removed:key"), repository.calls)
        assertFalse(repository.records.getValue("key").isActive)
        assertEquals(2_000L, repository.records.getValue("key").removedAtMillis)
    }

    @Test
    fun `removed then posted keeps the newer posted state`() = runTest {
        val repository = RecordingRepository()
        val processor = processor(repository)

        processor.enqueue(NotificationRepositoryEvent.Removed("key", 1_000))
        processor.enqueue(NotificationRepositoryEvent.Posted(record("key", postTimeMillis = 2_000)))
        processor.close()
        processor.join()

        assertEquals(listOf("removed:key", "posted:key"), repository.calls)
        assertTrue(repository.records.getValue("key").isActive)
        assertEquals(null, repository.records.getValue("key").removedAtMillis)
    }

    @Test
    fun `connect sync posted and removed are applied in enqueue order`() = runTest {
        val repository = RecordingRepository()
        repository.records["stale"] = record("stale")
        val processor = processor(repository)

        processor.enqueue(
            NotificationRepositoryEvent.SynchronizeActive(
                activeKeys = emptySet(),
                notifications = emptyList(),
                synchronizedAtMillis = 1_000
            )
        )
        processor.enqueue(NotificationRepositoryEvent.Posted(record("new", postTimeMillis = 2_000)))
        processor.enqueue(NotificationRepositoryEvent.Removed("new", 3_000))
        processor.close()
        processor.join()

        assertEquals(listOf("sync", "posted:new", "removed:new"), repository.calls)
        assertFalse(repository.records.getValue("stale").isActive)
        assertFalse(repository.records.getValue("new").isActive)
    }

    @Test
    fun `close drains queued events and rejects later events`() = runTest {
        val repository = RecordingRepository()
        val processor = processor(repository)

        processor.enqueue(NotificationRepositoryEvent.Posted(record("first")))
        processor.enqueue(NotificationRepositoryEvent.Removed("first", 2_000))
        processor.close()

        assertFalse(processor.enqueue(NotificationRepositoryEvent.Posted(record("late"))))
        processor.join()

        assertEquals(listOf("posted:first", "removed:first"), repository.calls)
        assertFalse(repository.records.getValue("first").isActive)
        assertFalse(repository.records.containsKey("late"))
    }

    @Test
    fun `repository failure reports only event type and processing continues`() = runTest {
        val repository = RecordingRepository().apply { failNextUpsert = true }
        val failures = mutableListOf<NotificationEventType>()
        val processor = NotificationEventProcessor(
            repository = repository,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            failureReporter = NotificationEventFailureReporter(failures::add)
        )

        processor.enqueue(
            NotificationRepositoryEvent.Posted(
                record(
                    key = "private-raw-key",
                    title = "private-title",
                    text = "private-body"
                )
            )
        )
        processor.enqueue(NotificationRepositoryEvent.Posted(record("safe")))
        processor.close()
        processor.join()

        assertEquals(listOf(NotificationEventType.POSTED), failures)
        assertEquals(setOf("safe"), repository.records.keys)
        assertFalse(failures.toString().contains("private"))
    }

    private fun kotlinx.coroutines.test.TestScope.processor(
        repository: NotificationRepository
    ): NotificationEventProcessor = NotificationEventProcessor(
        repository = repository,
        scope = backgroundScope,
        dispatcher = StandardTestDispatcher(testScheduler)
    )

    private fun record(
        key: String,
        postTimeMillis: Long = 1_000,
        title: String? = "title",
        text: String? = "text"
    ): NotificationRecord = NotificationRecord(
        key = key,
        packageName = "com.example.app",
        appLabel = "Example",
        title = title,
        text = text,
        postTimeMillis = postTimeMillis,
        notificationId = 1,
        tag = null,
        channelId = "test",
        category = NotificationDecision.HoldForDigest,
        reason = "test"
    )

    private class RecordingRepository : NotificationRepository {
        private val notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
        private val rules = MutableStateFlow<List<AppRule>>(emptyList())
        private val stats = MutableStateFlow(ClassificationStats())

        val records = linkedMapOf<String, NotificationRecord>()
        val calls = mutableListOf<String>()
        var beforeUpsert: suspend () -> Unit = {}
        var failNextUpsert: Boolean = false

        override fun observeNotifications(): Flow<List<NotificationItem>> = notifications
        override fun observeAppRules(): Flow<List<AppRule>> = rules
        override fun observeClassificationStats(): Flow<ClassificationStats> = stats

        override suspend fun upsert(notification: NotificationRecord) {
            beforeUpsert()
            if (failNextUpsert) {
                failNextUpsert = false
                throw IllegalStateException("synthetic failure")
            }
            calls += "posted:${notification.key}"
            records[notification.key] = notification
        }

        override suspend fun synchronizeActive(
            activeKeys: Set<String>,
            notifications: List<NotificationRecord>,
            synchronizedAtMillis: Long
        ) {
            calls += "sync"
            records.entries.forEach { entry ->
                val record = entry.value
                if (record.isActive && entry.key !in activeKeys) {
                    entry.setValue(
                        record.copy(isActive = false, removedAtMillis = synchronizedAtMillis)
                    )
                }
            }
            notifications.forEach { records[it.key] = it }
        }

        override suspend fun markRemoved(key: String, removedAtMillis: Long) {
            calls += "removed:$key"
            records[key]?.let {
                records[key] = it.copy(isActive = false, removedAtMillis = removedAtMillis)
            }
        }

        override suspend fun setPinned(key: String, pinned: Boolean) = Unit
        override suspend fun setNotificationDecision(key: String, decision: NotificationDecision?) = Unit
        override suspend fun setAppRule(
            packageName: String,
            appLabel: String,
            decision: NotificationDecision?
        ) = Unit
        override suspend fun delete(key: String) = Unit
        override suspend fun clearAll() = Unit
    }
}
