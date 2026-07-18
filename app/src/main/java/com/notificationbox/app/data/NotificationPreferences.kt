package com.notificationbox.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.DigestSchedule
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.notificationPrefsDataStore by preferencesDataStore("notification_box_prefs")

internal fun Flow<Preferences>.recoverPreferenceRead(): Flow<Preferences> =
    catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

data class NotificationPreferenceState(
    val mode: AppMode = AppMode.Observation,
    val onboardingCompleted: Boolean = false,
    val pausedUntilText: String = "解除まで",
    val digestSchedule: DigestSchedule = DigestSchedule()
)

class NotificationPreferences(context: Context) {
    private val dataStore = context.applicationContext.notificationPrefsDataStore

    private val modeKey = stringPreferencesKey("mode")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed_v1")
    private val pausedTextKey = stringPreferencesKey("paused_text")
    private val digestHourCountKey = intPreferencesKey("digest_hour_count")
    private val digestHourKeys = listOf(
        intPreferencesKey("digest_hour_1"),
        intPreferencesKey("digest_hour_2"),
        intPreferencesKey("digest_hour_3"),
        intPreferencesKey("digest_hour_4")
    )

    fun observeState(): Flow<NotificationPreferenceState> =
        dataStore.data
            .recoverPreferenceRead()
            .map { prefs ->
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
                    onboardingCompleted = prefs[onboardingCompletedKey] ?: false,
                    pausedUntilText = prefs[pausedTextKey]
                        ?.takeIf(String::isNotBlank)
                        ?: "解除まで",
                    digestSchedule = DigestSchedule(digestHours)
                )
            }

    suspend fun saveMode(mode: AppMode) {
        dataStore.edit { prefs ->
            prefs[modeKey] = mode.name
        }
    }

    suspend fun saveOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[onboardingCompletedKey] = completed
        }
    }

    suspend fun savePausedText(text: String) {
        dataStore.edit { prefs ->
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

        dataStore.edit { prefs ->
            prefs[digestHourCountKey] = normalized.size
            digestHourKeys.forEachIndexed { index, key ->
                prefs[key] = normalized.getOrNull(index)
                    ?: defaults.getOrElse(index) { defaults.last() }
            }
        }
    }
}
