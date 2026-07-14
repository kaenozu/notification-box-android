package com.notificationbox.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.DigestSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationPrefsDataStore by preferencesDataStore("notification_box_prefs")

data class NotificationPreferenceState(
    val mode: AppMode = AppMode.Observation,
    val pausedUntilText: String = "解除まで",
    val digestSchedule: DigestSchedule = DigestSchedule()
)

object NotificationPreferences {
    @Volatile
    private var appContext: Context? = null

    private val modeKey = stringPreferencesKey("mode")
    private val pausedTextKey = stringPreferencesKey("paused_text")
    private val digestHourCountKey = intPreferencesKey("digest_hour_count")
    private val digestHourKeys = listOf(
        intPreferencesKey("digest_hour_1"),
        intPreferencesKey("digest_hour_2"),
        intPreferencesKey("digest_hour_3"),
        intPreferencesKey("digest_hour_4")
    )

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun observeState(): Flow<NotificationPreferenceState> =
        requireContext().notificationPrefsDataStore.data.map { prefs ->
            val mode = prefs[modeKey]
                ?.let { stored -> runCatching { AppMode.valueOf(stored) }.getOrNull() }
                ?: AppMode.Observation
            val defaultHours = DigestSchedule().hours
            // Existing installations have no count key and historically persisted four slots.
            val storedCount = prefs[digestHourCountKey]
                ?.coerceIn(1, digestHourKeys.size)
                ?: digestHourKeys.size
            val digestHours = digestHourKeys
                .take(storedCount)
                .mapIndexed { index, key ->
                    prefs[key] ?: defaultHours.getOrElse(index) { defaultHours.last() }
                }
                .filter { it in 0..23 }
                .distinct()
                .ifEmpty { defaultHours }

            NotificationPreferenceState(
                mode = mode,
                pausedUntilText = prefs[pausedTextKey]
                    ?.takeIf(String::isNotBlank)
                    ?: "解除まで",
                digestSchedule = DigestSchedule(digestHours)
            )
        }

    suspend fun saveMode(mode: AppMode) {
        requireContext().notificationPrefsDataStore.edit { prefs ->
            prefs[modeKey] = mode.name
        }
    }

    suspend fun savePausedText(text: String) {
        requireContext().notificationPrefsDataStore.edit { prefs ->
            prefs[pausedTextKey] = text
        }
    }

    suspend fun saveDigestHours(hours: List<Int>) {
        val defaults = DigestSchedule().hours
        val normalized = hours
            .filter { it in 0..23 }
            .distinct()
            .take(digestHourKeys.size)
            .ifEmpty { defaults }

        requireContext().notificationPrefsDataStore.edit { prefs ->
            prefs[digestHourCountKey] = normalized.size
            digestHourKeys.forEachIndexed { index, key ->
                prefs[key] = normalized.getOrNull(index)
                    ?: defaults.getOrElse(index) { defaults.last() }
            }
        }
    }

    private fun requireContext(): Context =
        checkNotNull(appContext) { "NotificationPreferences not initialized" }
}
