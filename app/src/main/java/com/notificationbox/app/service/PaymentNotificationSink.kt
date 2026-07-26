package com.notificationbox.app.service

import com.notificationbox.app.data.repository.NotificationRecord
import com.notificationbox.app.data.repository.PaymentEventRecord
import com.notificationbox.app.data.repository.PaymentRepository
import com.notificationbox.app.domain.payment.PaymentNotificationInput
import com.notificationbox.app.domain.payment.PaymentParserRegistry
import kotlinx.coroutines.CancellationException

fun interface PaymentNotificationSink {
    suspend fun capture(notification: NotificationRecord)
}

object NoOpPaymentNotificationSink : PaymentNotificationSink {
    override suspend fun capture(notification: NotificationRecord) = Unit
}

class PaymentNotificationIngestor(
    private val repository: PaymentRepository,
    private val parserRegistry: PaymentParserRegistry = PaymentParserRegistry(),
    private val healthReporter: PaymentIngestionHealthReporter = PaymentIngestionHealthStore
) : PaymentNotificationSink {
    override suspend fun capture(notification: NotificationRecord) {
        val parsed = parserRegistry.parse(
            PaymentNotificationInput(
                packageName = notification.packageName,
                appLabel = notification.appLabel,
                title = notification.title,
                text = notification.text,
                postTimeMillis = notification.postTimeMillis
            )
        ) ?: return

        try {
            repository.upsert(
                PaymentEventRecord(
                    sourceNotificationKey = notification.key,
                    packageName = notification.packageName,
                    appLabel = notification.appLabel,
                    amountYen = parsed.amountYen,
                    merchantName = parsed.merchantName,
                    transactionType = parsed.transactionType,
                    occurredAtMillis = notification.postTimeMillis,
                    parserId = parsed.parserId,
                    parserVersion = parsed.parserVersion,
                    confidencePercent = parsed.confidencePercent
                )
            )
            healthReporter.recordSuccess()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            healthReporter.recordFailure()
            // Payment derivation remains additive: normal notification storage has already succeeded.
        }
    }
}
