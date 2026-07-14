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
            val digestHours = digestHourKeys
                .mapIndexed { index, key ->
                    prefs[key] ?: DigestSchedule().hours[index]
                }
                .filter { it in 0..23 }
                .distinct()
                .ifEmpty { DigestSchedule().hours }

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
        val normalized = hours
            .filter { it in 0..23 }
            .distinct()
            .take(digestHourKeys.size)
        val defaults = DigestSchedule().hours

        requireContext().notificationPrefsDataStore.edit { prefs ->
            digestHourKeys.forEachIndexed { index, key ->
                prefs[key] = normalized.getOrNull(index) ?: defaults[index]
            }
        }
    }

    private fun requireContext(): Context =
        checkNotNull(appContext) { "NotificationPreferences not initialized" }
}
