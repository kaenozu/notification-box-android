package com.notificationbox.app.data

import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.AppState
import com.notificationbox.app.model.DigestSchedule
import com.notificationbox.app.model.NotificationDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object NotificationStore {
    private val _state = MutableStateFlow(
        AppState()
    )

    val state: StateFlow<AppState> = _state.asStateFlow()

    fun setMode(mode: AppMode) {
        _state.update { it.copy(mode = mode) }
        NotificationPreferences.saveMode(mode.name)
    }

    fun setFilter(filter: NotificationDecision?) {
        _state.update { it.copy(selectedFilter = filter) }
    }

    fun pauseSummary(label: String) {
        _state.update { it.copy(pausedUntilText = label) }
        NotificationPreferences.savePausedText(label)
    }

    fun setDigestHours(hours: List<Int>) {
        _state.update { it.copy(digestSchedule = DigestSchedule(hours)) }
        NotificationPreferences.saveDigestHours(hours)
    }

    fun clearAll() {
        _state.update { it.copy(items = emptyList()) }
    }
}
