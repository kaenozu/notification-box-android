package com.notificationbox.app.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationbox.app.data.repository.PaymentEvent
import com.notificationbox.app.data.repository.PaymentRepository
import com.notificationbox.app.data.repository.PaymentSummary
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface PaymentUiState {
    data object Loading : PaymentUiState

    data class Content(
        val events: List<PaymentEvent>,
        val summary: PaymentSummary
    ) : PaymentUiState

    data object Error : PaymentUiState
}

class PaymentViewModel(
    repository: PaymentRepository,
    clock: Clock = Clock.systemUTC(),
    zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {
    private val periodStart = LocalDate.now(clock.withZone(zoneId))
        .withDayOfMonth(1)
        .atStartOfDay(zoneId)
        .toInstant()

    val uiState: StateFlow<PaymentUiState> =
        combine(
            repository.observeEvents(),
            repository.observeSummarySince(periodStart)
        ) { events, summary ->
            PaymentUiState.Content(events = events, summary = summary)
        }
            .catch { emit(PaymentUiState.Error) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                PaymentUiState.Loading
            )
}

class PaymentViewModelFactory(
    private val repository: PaymentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            return PaymentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}
