package com.notificationbox.app.data

import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.AppState
import com.notificationbox.app.model.DigestSchedule
import com.notificationbox.app.model.NotificationDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object NotificationStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(AppState())
    private var restoreJob: Job? = null

    val state: StateFlow<AppState> = mutableState.asStateFlow()

    @Synchronized
    fun initialize() {
        if (restoreJob != null) return
        restoreJob = scope.launch {
            NotificationPreferences.observeState().collectLatest { preferences ->
                mutableState.update { current ->
                    current.copy(
                        mode = preferences.mode,
                        preferencesLoaded = true,
                        onboardingCompleted = preferences.onboardingCompleted,
                        pausedUntilText = preferences.pausedUntilText,
                        digestSchedule = preferences.digestSchedule
                    )
                }
            }
        }
    }

    fun setMode(mode: AppMode) {
        mutableState.update { it.copy(mode = mode) }
        scope.launch {
            runCatching { NotificationPreferences.saveMode(mode) }
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        mutableState.update { it.copy(onboardingCompleted = completed) }
        scope.launch {
            runCatching { NotificationPreferences.saveOnboardingCompleted(completed) }
        }
    }

    fun setFilter(filter: NotificationDecision?) {
        mutableState.update { it.copy(selectedFilter = filter) }
    }

    fun pauseSummary(label: String) {
        mutableState.update { it.copy(pausedUntilText = label) }
        scope.launch {
            runCatching { NotificationPreferences.savePausedText(label) }
        }
    }

    fun setDigestHours(hours: List<Int>) {
        val normalized = hours.filter { it in 0..23 }.distinct()
        val schedule = DigestSchedule(
            hours = normalized.ifEmpty { DigestSchedule().hours }
        )
        mutableState.update { it.copy(digestSchedule = schedule) }
        scope.launch {
            runCatching { NotificationPreferences.saveDigestHours(schedule.hours) }
        }
    }
}
