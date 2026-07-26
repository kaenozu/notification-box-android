package com.notificationbox.app.data.db

data class PaymentSummaryRow(
    val eventCount: Int,
    val purchaseTotalYen: Long,
    val refundTotalYen: Long,
    val needsReviewCount: Int
)
