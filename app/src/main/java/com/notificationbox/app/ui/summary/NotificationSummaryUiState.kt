/*
 * File: app/src/main/java/com/notificationbox/app/ui/summary/NotificationSummaryUiState.kt
 * Description: Exhaustive UI states for the notification-summary screen.
 * Related: NotificationSummaryViewModel.kt, NotificationSummaryScreen.kt, NotificationSummary.kt
 */
package com.notificationbox.app.ui.summary

import com.notificationbox.app.model.NotificationSummary

sealed interface NotificationSummaryUiState {
    data object Loading : NotificationSummaryUiState
    data class Content(val summary: NotificationSummary) : NotificationSummaryUiState
    data object Empty : NotificationSummaryUiState
    data object Error : NotificationSummaryUiState
}
