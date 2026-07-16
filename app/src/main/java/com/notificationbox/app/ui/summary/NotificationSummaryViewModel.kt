package com.notificationbox.app.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.model.NotificationSummary
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NotificationSummaryViewModel(
    private val repository: NotificationRepository,
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {

    private val periodStart = clock.instant().minus(Duration.ofHours(24))

    val uiState: StateFlow<NotificationSummaryUiState> =
        repository.observeSummarySince(periodStart)
            .map<NotificationSummary, NotificationSummaryUiState> { summary ->
                if (summary.totalCount == 0) {
                    NotificationSummaryUiState.Empty
                } else {
                    NotificationSummaryUiState.Content(summary)
                }
            }
            .catch { error ->
                emit(
                    NotificationSummaryUiState.Error(
                        error.message ?: "通知サマリーを読み込めませんでした"
                    )
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                NotificationSummaryUiState.Loading
            )
}

class NotificationSummaryViewModelFactory(
    private val repository: NotificationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationSummaryViewModel::class.java)) {
            return NotificationSummaryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
