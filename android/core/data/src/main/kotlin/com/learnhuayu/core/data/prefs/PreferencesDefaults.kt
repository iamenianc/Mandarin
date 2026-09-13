package com.learnhuayu.core.data.prefs

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

data class PreferencesDefaults(
    val dailyGoalMinutes: Int?,
    val sessionLengthMinutes: Int?,
    val showHangul: Boolean,
    val recordingsToProviderConsent: Boolean,
    val consentVersion: String,
)

internal fun parsePreferencesDefaults(
    json: Json,
    text: String,
): PreferencesDefaults {
    val file = json.decodeFromString<PreferencesDefaultsFileDto>(text)
    val byKey = file.preferences.associateBy { entry -> entry.key }
    return PreferencesDefaults(
        dailyGoalMinutes = byKey[PreferenceKeys.DAILY_GOAL_MINUTES]?.default?.asInt(),
        sessionLengthMinutes = byKey[PreferenceKeys.SESSION_LENGTH_MINUTES]?.default?.asInt(),
        showHangul = byKey[PreferenceKeys.SHOW_HANGUL]?.default?.asBoolean() ?: false,
        recordingsToProviderConsent =
        byKey[PreferenceKeys.RECORDINGS_TO_PROVIDER]?.default?.asBoolean() ?: false,
        consentVersion = byKey[PreferenceKeys.CONSENT_VERSION]?.default?.asString() ?: "0",
    )
}

@Serializable
private data class PreferencesDefaultsFileDto(
    val schemaVersion: Int = 1,
    val preferences: List<PreferenceDefaultDto> = emptyList(),
)

@Serializable
private data class PreferenceDefaultDto(
    val key: String,
    val type: String,
    val default: JsonElement? = null,
)

private fun JsonElement?.asInt(): Int? = (this as? JsonPrimitive)?.intOrNull

private fun JsonElement?.asBoolean(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull

private fun JsonElement?.asString(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.content.takeIf { primitive.isString }
}
