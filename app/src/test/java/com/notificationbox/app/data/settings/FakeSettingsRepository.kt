package com.notificationbox.app.data.settings

import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.DigestSchedule
import com.notificationbox.app.model.NotificationDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(
    initial: AppSettings = AppSettings(preferencesLoaded = true)
) : SettingsRepository {
    private val mutableSettings = MutableStateFlow(initial)
    override val settings: StateFlow<AppSettings> = mutableSettings

    override suspend fun setMode(mode: AppMode) {
        mutableSettings.update { it.copy(mode = mode) }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        mutableSettings.update { it.copy(onboardingCompleted = completed) }
    }

    override fun setFilter(filter: NotificationDecision?) {
        mutableSettings.update { it.copy(selectedFilter = filter) }
    }

    override suspend fun pauseSummary(label: String) {
        mutableSettings.update { it.copy(pausedUntilText = label) }
    }

    override suspend fun setDigestHours(hours: List<Int>) {
        val normalized = hours.filter { it in 0..23 }.distinct()
        mutableSettings.update {
            it.copy(
                digestSchedule = DigestSchedule(
                    normalized.ifEmpty { DigestSchedule().hours }
                )
            )
        }
    }
}
