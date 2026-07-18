/*
 * File: app/src/main/java/com/notificationbox/app/data/repository/NotificationSummaryRepository.kt
 * Description: Narrow summary-observation contract used by the summary ViewModel.
 * Related: NotificationRepository.kt, RoomNotificationRepository.kt, NotificationSummaryViewModel.kt
 */
package com.notificationbox.app.data.repository

import com.notificationbox.app.model.NotificationSummary
import java.time.Instant
import kotlinx.coroutines.flow.Flow

fun interface NotificationSummarySource {
    fun observeSummarySince(since: Instant): Flow<NotificationSummary>
}
