package com.learnhuayu.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setDailyGoalMinutes(minutes: Int?)

    suspend fun setSessionLengthMinutes(minutes: Int?)

    suspend fun setShowHangul(enabled: Boolean)

    suspend fun setRecordingsToProviderConsent(granted: Boolean)

    suspend fun setConsentVersion(version: String)
}

@Singleton
internal class DataStorePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val defaultsSource: PreferencesDefaultsSource,
) : PreferencesRepository {
    override val preferences: Flow<UserPreferences> =
        flow {
            val defaults = defaultsSource.defaults()
            emitAll(dataStore.data.map { stored -> stored.toUserPreferences(defaults) })
        }

    override suspend fun setDailyGoalMinutes(minutes: Int?) {
        dataStore.edit { stored ->
            if (minutes == null) {
                stored.remove(PreferenceKeys.dailyGoalMinutes)
            } else {
                stored[PreferenceKeys.dailyGoalMinutes] = minutes
            }
        }
    }

    override suspend fun setSessionLengthMinutes(minutes: Int?) {
        dataStore.edit { stored ->
            if (minutes == null) {
                stored.remove(PreferenceKeys.sessionLengthMinutes)
            } else {
                stored[PreferenceKeys.sessionLengthMinutes] = minutes
            }
        }
    }

    override suspend fun setShowHangul(enabled: Boolean) {
        dataStore.edit { stored -> stored[PreferenceKeys.showHangul] = enabled }
    }

    override suspend fun setRecordingsToProviderConsent(granted: Boolean) {
        dataStore.edit { stored -> stored[PreferenceKeys.recordingsToProvider] = granted }
    }

    override suspend fun setConsentVersion(version: String) {
        dataStore.edit { stored -> stored[PreferenceKeys.consentVersion] = version }
    }
}

private fun Preferences.toUserPreferences(defaults: PreferencesDefaults): UserPreferences = UserPreferences(
    dailyGoalMinutes = this[PreferenceKeys.dailyGoalMinutes] ?: defaults.dailyGoalMinutes,
    sessionLengthMinutes = this[PreferenceKeys.sessionLengthMinutes] ?: defaults.sessionLengthMinutes,
    showHangul = this[PreferenceKeys.showHangul] ?: defaults.showHangul,
    recordingsToProviderConsent =
    this[PreferenceKeys.recordingsToProvider] ?: defaults.recordingsToProviderConsent,
    consentVersion = this[PreferenceKeys.consentVersion] ?: defaults.consentVersion,
)
