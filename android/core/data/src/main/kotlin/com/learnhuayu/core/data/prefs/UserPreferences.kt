package com.learnhuayu.core.data.prefs

data class UserPreferences(
    val dailyGoalMinutes: Int?,
    val sessionLengthMinutes: Int?,
    val showHangul: Boolean,
    val recordingsToProviderConsent: Boolean,
    val consentVersion: String,
)
