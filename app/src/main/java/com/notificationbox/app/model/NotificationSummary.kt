/*
 * File: app/src/main/java/com/notificationbox/app/model/NotificationSummary.kt
 * Description: Domain model for notification counts generated over a bounded time period.
 * Related: NotificationSummaryRow.kt, RoomNotificationRepository.kt, NotificationSummaryUiState.kt
 */
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
