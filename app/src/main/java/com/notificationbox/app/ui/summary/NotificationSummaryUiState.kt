package com.notificationbox.app.ui.summary

import com.notificationbox.app.model.NotificationSummary

sealed interface NotificationSummaryUiState {
    data object Loading : NotificationSummaryUiState
    data class Content(val summary: NotificationSummary) : NotificationSummaryUiState
    data object Empty : NotificationSummaryUiState
    data class Error(val message: String) : NotificationSummaryUiState
}
