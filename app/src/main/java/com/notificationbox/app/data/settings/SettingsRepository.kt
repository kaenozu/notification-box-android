package com.notificationbox.app.data.settings

import com.notificationbox.app.data.NotificationPreferences
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.DigestSchedule
import com.notificationbox.app.model.NotificationDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Application settings exposed independently from notification-history state. */
data class AppSettings(
    val mode: AppMode = AppMode.Observation,
    val preferencesLoaded: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val pausedUntilText: String = "解除まで",
    val digestSchedule: DigestSchedule = DigestSchedule(),
    val selectedFilter: NotificationDecision? = null
)

interface SettingsRepository {
    val settings: StateFlow<AppSettings>

    suspend fun setMode(mode: AppMode)

    suspend fun setOnboardingCompleted(completed: Boolean)

    fun setFilter(filter: NotificationDecision?)

    suspend fun pauseSummary(label: String)

    suspend fun setDigestHours(hours: List<Int>)
}

class DataStoreSettingsRepository(
    private val preferences: NotificationPreferences,
    scope: CoroutineScope
) : SettingsRepository {
    private val selectedFilter = MutableStateFlow<NotificationDecision?>(null)

    override val settings: StateFlow<AppSettings> = combine(
        preferences.observeState(),
        selectedFilter
    ) { persisted, filter ->
        AppSettings(
            mode = persisted.mode,
            preferencesLoaded = true,
            onboardingCompleted = persisted.onboardingCompleted,
            pausedUntilText = persisted.pausedUntilText,
            digestSchedule = persisted.digestSchedule,
            selectedFilter = filter
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    override suspend fun setMode(mode: AppMode) {
        preferences.saveMode(mode)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferences.saveOnboardingCompleted(completed)
    }

    override fun setFilter(filter: NotificationDecision?) {
        selectedFilter.value = filter
    }

    override suspend fun pauseSummary(label: String) {
        preferences.savePausedText(label)
    }

    override suspend fun setDigestHours(hours: List<Int>) {
        val normalized = hours.filter { it in 0..23 }.distinct()
        val schedule = DigestSchedule(
            hours = normalized.ifEmpty { DigestSchedule().hours }
        )
        preferences.saveDigestHours(schedule.hours)
    }
}
