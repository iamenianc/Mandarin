package com.learnhuayu.core.data.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object PreferenceKeys {
    const val DAILY_GOAL_MINUTES = "dailyGoalMinutes"
    const val SESSION_LENGTH_MINUTES = "sessionLengthMinutes"
    const val SHOW_HANGUL = "showHangul"
    const val RECORDINGS_TO_PROVIDER = "consent.recordingsToProvider"
    const val CONSENT_VERSION = "consent.version"

    val dailyGoalMinutes = intPreferencesKey(DAILY_GOAL_MINUTES)
    val sessionLengthMinutes = intPreferencesKey(SESSION_LENGTH_MINUTES)
    val showHangul = booleanPreferencesKey(SHOW_HANGUL)
    val recordingsToProvider = booleanPreferencesKey(RECORDINGS_TO_PROVIDER)
    val consentVersion = stringPreferencesKey(CONSENT_VERSION)
}
