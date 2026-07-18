package com.notificationbox.app.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationbox.app.data.repository.NotificationRepository
import com.notificationbox.app.data.repository.NotificationSummarySource
import com.notificationbox.app.model.NotificationSummary
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSummaryViewModel(
    private val summarySource: NotificationSummarySource,
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {

    private val periodStart = MutableStateFlow(currentPeriodStart())

    val uiState: StateFlow<NotificationSummaryUiState> =
        periodStart
            .flatMapLatest { since ->
                summarySource.observeSummarySince(since)
                    .map<NotificationSummary, NotificationSummaryUiState> { summary ->
                        if (summary.totalCount == 0) {
                            NotificationSummaryUiState.Empty
                        } else {
                            NotificationSummaryUiState.Content(summary)
                        }
                    }
                    .catch { emit(NotificationSummaryUiState.Error) }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                NotificationSummaryUiState.Loading
            )

    fun refresh() {
        periodStart.value = currentPeriodStart()
    }

    private fun currentPeriodStart() = clock.instant().minus(Duration.ofHours(24))
}

class NotificationSummaryViewModelFactory(
    private val repository: NotificationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationSummaryViewModel::class.java)) {
            return NotificationSummaryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}
