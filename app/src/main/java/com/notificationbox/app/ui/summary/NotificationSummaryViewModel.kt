package com.notificationbox.app.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.data.repository.NotificationSummaryRepository
import com.notificationbox.app.data.repository.NotificationSummarySource
import com.notificationbox.app.model.NotificationSummary
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NotificationSummaryViewModel(
    summarySource: NotificationSummarySource,
    clock: Clock = Clock.systemUTC()
) : ViewModel() {

    private val periodStart = clock.instant().minus(Duration.ofHours(24))

    val uiState: StateFlow<NotificationSummaryUiState> =
        summarySource.observeSummarySince(periodStart)
            .map<NotificationSummary, NotificationSummaryUiState> { summary ->
                if (summary.totalCount == 0) {
                    NotificationSummaryUiState.Empty
                } else {
                    NotificationSummaryUiState.Content(summary)
                }
            }
            .catch { emit(NotificationSummaryUiState.Error) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                NotificationSummaryUiState.Loading
            )
}

class NotificationSummaryViewModelFactory(
    repository: NotificationRepository
) : ViewModelProvider.Factory {
    private val summarySource = NotificationSummaryRepository(repository)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationSummaryViewModel::class.java)) {
            return NotificationSummaryViewModel(summarySource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}
