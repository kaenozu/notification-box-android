/*
 * File: app/src/main/java/com/notificationbox/app/data/db/NotificationSummaryRow.kt
 * Description: Room projection for notification summary aggregate counts.
 * Related: NotificationDao.kt, RoomNotificationRepository.kt, NotificationSummary.kt
 */
package com.notificationbox.app.data.db

data class NotificationSummaryRow(
    val totalCount: Int,
    val keepNowCount: Int,
    val holdForDigestCount: Int,
    val ignoreCount: Int
)
