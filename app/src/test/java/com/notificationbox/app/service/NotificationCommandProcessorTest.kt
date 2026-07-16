package com.notificationbox.app.service

import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.model.AppRule
import com.notificationbox.app.model.ClassificationStats
import com.notificationbox.app.model.IngestionErrorCode
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationIngestionHealth
import com.notificationbox.app.model.NotificationItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationCommandProcessorTest {
    @Test
    fun `queue applies commands in submission order`() = runTest {
        val repository = RecordingRepository()
        val health = RecordingHealthReporter()
        val queue = NotificationCommandQueue(
            scope = this,
            processor = NotificationCommandProcessor(repository, health),
            healthReporter = health
        )

        queue.submit(NotificationCommand.Upsert(record("same")))
        queue.submit(NotificationCommand.MarkRemoved("same", 2_000L))
        queue.submit(NotificationCommand.Upsert(record("same", postTimeMillis = 3_000L)))
        advanceUntilIdle()

        assertEquals(
            listOf("upsert:same:1000", "remove:same:2000", "upsert:same:3000"),
            repository.operations
        )
        assertEquals(3L, health.health.value.processedCommands)
        assertEquals(0L, health.health.value.failedCommands)
        queue.close()
        queue.join()
    }

    @Test
    fun `repository failure is counted and later commands continue`() = runTest {
        val repository = RecordingRepository(failFirstUpsert = true)
        val health = RecordingHealthReporter()
        val queue = NotificationCommandQueue(
            scope = this,
            processor = NotificationCommandProcessor(repository, health),
            healthReporter = health
        )

        queue.submit(NotificationCommand.Upsert(record("failed")))
        queue.submit(NotificationCommand.MarkRemoved("next", 4_000L))
        advanceUntilIdle()

        assertEquals(listOf("upsert:failed:1000", "remove:next:4000"), repository.operations)
        assertEquals(1L, health.health.value.processedCommands)
        assertEquals(1L, health.health.value.failedCommands)
        assertEquals(IngestionErrorCode.REPOSITORY_OPERATION_FAILED, health.health.value.lastError)
        queue.close()
        queue.join()
    }

    @Test
    fun `bounded queue reports overflow and requests reconciliation`() = runTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repository = RecordingRepository(
            beforeUpsert = {
                started.complete(Unit)
                release.await()
            }
        )
        val health = RecordingHealthReporter()
        var reconciliationRequests = 0
        val queue = NotificationCommandQueue(
            scope = backgroundScope,
            processor = NotificationCommandProcessor(repository, health),
            healthReporter = health,
            capacity = 1,
            onOverflow = { reconciliationRequests++ }
        )

        assertTrue(queue.submit(NotificationCommand.Upsert(record("processing"))))
        runCurrent()
        started.await()
        assertTrue(queue.submit(NotificationCommand.Upsert(record("queued"))))
        assertFalse(queue.submit(NotificationCommand.Upsert(record("overflow"))))

        assertEquals(1, reconciliationRequests)
        assertEquals(1L, health.health.value.failedCommands)
        assertEquals(IngestionErrorCode.COMMAND_QUEUE_OVERFLOW, health.health.value.lastError)

        release.complete(Unit)
        advanceUntilIdle()
        queue.close()
        queue.join()
    }

    @Test
    fun `close drains accepted command and rejects later submission`() = runTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repository = RecordingRepository(
            beforeUpsert = {
                started.complete(Unit)
                release.await()
            }
        )
        val health = RecordingHealthReporter()
        val queue = NotificationCommandQueue(
            scope = backgroundScope,
            processor = NotificationCommandProcessor(repository, health),
            healthReporter = health
        )

        assertTrue(queue.submit(NotificationCommand.Upsert(record("accepted"))))
        runCurrent()
        started.await()

        queue.close()
        assertFalse(queue.submit(NotificationCommand.Upsert(record("late"))))
        release.complete(Unit)
        advanceUntilIdle()
        queue.join()

        assertEquals(listOf("upsert:accepted:1000"), repository.operations)
        assertEquals(1L, health.health.value.processedCommands)
        assertEquals(1L, health.health.value.failedCommands)
        assertEquals(IngestionErrorCode.COMMAND_QUEUE_CLOSED, health.health.value.lastError)
    }

    private fun record(key: String, postTimeMillis: Long = 1_000L) =
        NotificationRecord(
            key = key,
            packageName = "com.example",
            appLabel = "Example",
            title = "title",
            text = "text",
            postTimeMillis = postTimeMillis,
            notificationId = 1,
            tag = null,
            channelId = "channel",
            category = NotificationDecision.KeepNow,
            reason = "test"
        )

    private class RecordingHealthReporter : NotificationIngestionHealthReporter {
        private val mutableHealth = MutableStateFlow(NotificationIngestionHealth())
        override val health: StateFlow<NotificationIngestionHealth> = mutableHealth

        override fun recordSuccess() {
            mutableHealth.value = mutableHealth.value.copy(
                processedCommands = mutableHealth.value.processedCommands + 1
            )
        }

        override fun recordFailure(code: IngestionErrorCode) {
            mutableHealth.value = mutableHealth.value.copy(
                failedCommands = mutableHealth.value.failedCommands + 1,
                lastError = code
            )
        }
    }

    private class RecordingRepository(
        private val failFirstUpsert: Boolean = false,
        private val beforeUpsert: suspend () -> Unit = {}
    ) : NotificationRepository {
        val operations = mutableListOf<String>()
        private var failed = false

        override fun observeNotifications(): Flow<List<NotificationItem>> =
            MutableStateFlow(emptyList())

        override fun observeAppRules(): Flow<List<AppRule>> =
            MutableStateFlow(emptyList())

        override fun observeClassificationStats(): Flow<ClassificationStats> =
            MutableStateFlow(ClassificationStats())

        override suspend fun upsert(notification: NotificationRecord) {
            beforeUpsert()
            operations += "upsert:${notification.key}:${notification.postTimeMillis}"
            if (failFirstUpsert && !failed) {
                failed = true
                error("forced failure")
            }
        }

        override suspend fun synchronizeActive(
            activeKeys: Set<String>,
            notifications: List<NotificationRecord>,
            synchronizedAtMillis: Long
        ) {
            operations += "sync:$synchronizedAtMillis"
        }

        override suspend fun markRemoved(key: String, removedAtMillis: Long) {
            operations += "remove:$key:$removedAtMillis"
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
        override suspend fun pruneExpired() = Unit
        override suspend fun resetClassificationStats() = Unit
    }
}
