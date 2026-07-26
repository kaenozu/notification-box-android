package com.notificationbox.app.data.repository

import com.notificationbox.app.domain.payment.PaymentTransactionType
import java.time.Instant
import kotlinx.coroutines.flow.Flow

data class PaymentEventRecord(
    val sourceNotificationKey: String,
    val packageName: String,
    val appLabel: String,
    val amountYen: Long,
    val merchantName: String?,
    val transactionType: PaymentTransactionType,
    val occurredAtMillis: Long,
    val parserId: String,
    val parserVersion: Int,
    val confidencePercent: Int
)

data class PaymentEvent(
    val sourceNotificationKey: String,
    val packageName: String,
    val appLabel: String,
    val amountYen: Long,
    val merchantName: String?,
    val transactionType: PaymentTransactionType,
    val occurredAt: Instant,
    val confidencePercent: Int,
    val status: String
)

data class PaymentSummary(
    val eventCount: Int,
    val purchaseTotalYen: Long,
    val refundTotalYen: Long,
    val needsReviewCount: Int,
    val periodStart: Instant,
    val generatedAt: Instant
) {
    val netSpendYen: Long
        get() = purchaseTotalYen - refundTotalYen
}

interface PaymentRepository {
    fun observeEvents(): Flow<List<PaymentEvent>>

    fun observeSummarySince(since: Instant): Flow<PaymentSummary>

    suspend fun upsert(record: PaymentEventRecord)
}
