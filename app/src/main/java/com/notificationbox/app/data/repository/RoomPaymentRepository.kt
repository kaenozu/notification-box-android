package com.notificationbox.app.data.repository

import com.notificationbox.app.data.db.NotificationDatabase
import com.notificationbox.app.data.db.PaymentEventEntity
import com.notificationbox.app.domain.payment.PaymentTransactionType
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class RoomPaymentRepository(
    database: NotificationDatabase,
    private val clock: Clock = Clock.systemUTC()
) : PaymentRepository {
    private val paymentEventDao = database.paymentEventDao()

    override fun observeEvents(): Flow<List<PaymentEvent>> =
        paymentEventDao.observeAll()
            .map { entities -> entities.map { entity -> entity.toModel() } }
            .flowOn(Dispatchers.Default)

    override fun observeSummarySince(since: Instant): Flow<PaymentSummary> =
        paymentEventDao.observeSummarySince(since.toEpochMilli()).map { row ->
            PaymentSummary(
                eventCount = row.eventCount,
                purchaseTotalYen = row.purchaseTotalYen,
                refundTotalYen = row.refundTotalYen,
                needsReviewCount = row.needsReviewCount,
                periodStart = since,
                generatedAt = clock.instant()
            )
        }

    override suspend fun upsert(record: PaymentEventRecord) {
        paymentEventDao.upsert(record.toEntity())
    }

    override suspend fun clearAll() {
        paymentEventDao.clearAll()
    }

    private fun PaymentEventRecord.toEntity(): PaymentEventEntity =
        PaymentEventEntity(
            sourceNotificationKey = sourceNotificationKey,
            packageName = packageName,
            appLabel = appLabel,
            amountYen = amountYen,
            merchantName = merchantName,
            transactionType = transactionType.name,
            occurredAtMillis = occurredAtMillis,
            parserId = parserId,
            parserVersion = parserVersion,
            confidencePercent = confidencePercent
        )

    private fun PaymentEventEntity.toModel(): PaymentEvent =
        PaymentEvent(
            sourceNotificationKey = sourceNotificationKey,
            packageName = packageName,
            appLabel = appLabel,
            amountYen = amountYen,
            merchantName = merchantName,
            transactionType = runCatching {
                PaymentTransactionType.valueOf(transactionType)
            }.getOrDefault(PaymentTransactionType.UNKNOWN),
            occurredAt = Instant.ofEpochMilli(occurredAtMillis),
            confidencePercent = confidencePercent,
            status = status
        )
}
