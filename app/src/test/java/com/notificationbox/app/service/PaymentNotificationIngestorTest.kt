package com.notificationbox.app.service

import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.PaymentEvent
import com.notificationbox.app.data.repository.PaymentEventRecord
import com.notificationbox.app.data.repository.PaymentRepository
import com.notificationbox.app.data.repository.PaymentSummary
import com.notificationbox.app.domain.payment.PayPayNotificationParser
import com.notificationbox.app.model.NotificationContentAvailability
import com.notificationbox.app.model.NotificationDecision
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentNotificationIngestorTest {
    @Test
    fun `reports payment storage failure without throwing`() = runTest {
        val reporter = FakePaymentHealthReporter()
        val ingestor = PaymentNotificationIngestor(
            repository = FailingPaymentRepository(),
            healthReporter = reporter
        )

        ingestor.capture(payPayNotification())

        assertEquals(0L, reporter.health.value.parsedEvents)
        assertEquals(1L, reporter.health.value.failedEvents)
    }

    @Test
    fun `records successful derived event`() = runTest {
        val reporter = FakePaymentHealthReporter()
        val repository = RecordingPaymentRepository()
        val ingestor = PaymentNotificationIngestor(
            repository = repository,
            healthReporter = reporter
        )

        ingestor.capture(payPayNotification())

        assertEquals(1_280L, repository.recorded?.amountYen)
        assertEquals(1L, reporter.health.value.parsedEvents)
        assertEquals(0L, reporter.health.value.failedEvents)
    }

    private fun payPayNotification() = NotificationRecord(
        key = "payment-key",
        packageName = PayPayNotificationParser.PAYPAY_PACKAGE_NAME,
        appLabel = "PayPay",
        title = "支払い完了",
        text = "利用先：テスト店舗\n1,280円のお支払いが完了しました",
        postTimeMillis = 1_000L,
        notificationId = 1,
        tag = null,
        channelId = null,
        category = NotificationDecision.HoldForDigest,
        reason = "test",
        contentAvailability = NotificationContentAvailability.AVAILABLE
    )

    private class FakePaymentHealthReporter : PaymentIngestionHealthReporter {
        override val health = MutableStateFlow(PaymentIngestionHealth())

        override fun recordSuccess() {
            health.value = health.value.copy(parsedEvents = health.value.parsedEvents + 1)
        }

        override fun recordFailure() {
            health.value = health.value.copy(failedEvents = health.value.failedEvents + 1)
        }
    }

    private class RecordingPaymentRepository : BasePaymentRepository() {
        var recorded: PaymentEventRecord? = null

        override suspend fun upsert(record: PaymentEventRecord) {
            recorded = record
        }

        override suspend fun updateTransactionType(
            sourceNotificationKey: String,
            transactionType: com.notificationbox.app.domain.payment.PaymentTransactionType
        ) = Unit
    }

    private class FailingPaymentRepository : BasePaymentRepository() {
        override suspend fun upsert(record: PaymentEventRecord) {
            error("synthetic storage failure")
        }

        override suspend fun updateTransactionType(
            sourceNotificationKey: String,
            transactionType: com.notificationbox.app.domain.payment.PaymentTransactionType
        ) = Unit
    }

    private abstract class BasePaymentRepository : PaymentRepository {
        override fun observeEvents(): Flow<List<PaymentEvent>> = flowOf(emptyList())

        override fun observeSummarySince(since: Instant): Flow<PaymentSummary> = flowOf(
            PaymentSummary(
                eventCount = 0,
                purchaseTotalYen = 0,
                refundTotalYen = 0,
                needsReviewCount = 0,
                periodStart = since,
                generatedAt = since
            )
        )

        override suspend fun clearAll() = Unit
    }
}
