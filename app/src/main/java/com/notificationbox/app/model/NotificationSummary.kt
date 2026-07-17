package com.notificationbox.app.model

import java.time.Instant

data class NotificationSummary(
    val totalCount: Int,
    val keepNowCount: Int,
    val holdForDigestCount: Int,
    val ignoreCount: Int,
    val periodStart: Instant,
    val generatedAt: Instant
)
