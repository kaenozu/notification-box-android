package com.notificationbox.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.notificationPrefsDataStore by preferencesDataStore("notification_box_prefs")

object NotificationPreferences {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var appContext: Context? = null
    @Volatile private var initialized = false
    private val _ready = MutableStateFlow(false)

    private val modeKey = stringPreferencesKey("mode")
    private val pausedTextKey = stringPreferencesKey("paused_text")
    private val listenerGrantedKey = booleanPreferencesKey("listener_granted")
    private val postGrantedKey = booleanPreferencesKey("post_granted")
    private val digestHour1Key = intPreferencesKey("digest_hour_1")
    private val digestHour2Key = intPreferencesKey("digest_hour_2")
    private val digestHour3Key = intPreferencesKey("digest_hour_3")
    private val digestHour4Key = intPreferencesKey("digest_hour_4")

    fun initialize(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true
        _ready.value = true
    }

    fun ready(): StateFlow<Boolean> = _ready

    fun observeMode(): Flow<String> = requireContext().notificationPrefsDataStore.data.map { prefs ->
        prefs[modeKey] ?: "Observation"
    }

    suspend fun loadPausedText(): String = requireContext().notificationPrefsDataStore.data.first()[pausedTextKey] ?: "解除まで"

    fun saveMode(mode: String) {
        scope.launch {
            requireContext().notificationPrefsDataStore.edit { it[modeKey] = mode }
        }
    }

    fun savePausedText(text: String) {
        scope.launch {
            requireContext().notificationPrefsDataStore.edit { it[pausedTextKey] = text }
        }
    }

    fun savePermissions(listenerGranted: Boolean, postGranted: Boolean) {
        scope.launch {
            requireContext().notificationPrefsDataStore.edit {
            it[listenerGrantedKey] = listenerGranted
            it[postGrantedKey] = postGranted
            }
        }
    }

    fun saveDigestHours(hours: List<Int>) {
        scope.launch {
            requireContext().notificationPrefsDataStore.edit {
            it[digestHour1Key] = hours.getOrNull(0) ?: 8
            it[digestHour2Key] = hours.getOrNull(1) ?: 12
            it[digestHour3Key] = hours.getOrNull(2) ?: 18
            it[digestHour4Key] = hours.getOrNull(3) ?: 21
            }
        }
    }

    private fun requireContext(): Context =
        checkNotNull(appContext) { "NotificationPreferences not initialized" }
}
