package com.notificationbox.app.data.db

data class NotificationSummaryRow(
    val totalCount: Int,
    val keepNowCount: Int,
    val holdForDigestCount: Int,
    val ignoreCount: Int
)
