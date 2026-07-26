package com.notificationbox.app.domain.payment

enum class PaymentTransactionType {
    PURCHASE,
    REFUND,
    CHARGE,
    TRANSFER_OUT,
    TRANSFER_IN,
    UNKNOWN
}

data class PaymentNotificationInput(
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postTimeMillis: Long
)

data class PaymentParseResult(
    val amountYen: Long,
    val merchantName: String?,
    val transactionType: PaymentTransactionType,
    val confidencePercent: Int,
    val parserId: String,
    val parserVersion: Int
)

interface PaymentNotificationParser {
    fun supports(packageName: String): Boolean

    fun parse(input: PaymentNotificationInput): PaymentParseResult?
}

class PaymentParserRegistry(
    private val parsers: List<PaymentNotificationParser> = listOf(
        PayPayNotificationParser()
    )
) {
    fun parse(input: PaymentNotificationInput): PaymentParseResult? =
        parsers.firstOrNull { parser -> parser.supports(input.packageName) }
            ?.parse(input)
}
