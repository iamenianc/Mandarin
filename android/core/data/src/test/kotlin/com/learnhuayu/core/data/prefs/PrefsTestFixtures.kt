package com.learnhuayu.core.data.prefs

internal val defaultsJson =
    """
    {
      "schemaVersion": 1,
      "status": "proposed",
      "preferences": [
        {
          "key": "dailyGoalMinutes",
          "type": "integer",
          "default": null
        },
        {
          "key": "sessionLengthMinutes",
          "type": "integer",
          "default": null
        },
        {
          "key": "showHangul",
          "type": "boolean",
          "default": false
        },
        {
          "key": "consent.recordingsToProvider",
          "type": "boolean",
          "default": false
        },
        {
          "key": "consent.version",
          "type": "string",
          "default": "0"
        }
      ]
    }
    """.trimIndent()

internal val configuredDefaultsJson =
    """
    {
      "schemaVersion": 1,
      "preferences": [
        { "key": "dailyGoalMinutes", "type": "integer", "default": 15 },
        { "key": "sessionLengthMinutes", "type": "integer", "default": 10 },
        { "key": "showHangul", "type": "boolean", "default": true },
        { "key": "consent.recordingsToProvider", "type": "boolean", "default": true },
        { "key": "consent.version", "type": "string", "default": "1" }
      ]
    }
    """.trimIndent()

internal class FixedPreferencesDefaultsSource(
    private val defaults: PreferencesDefaults,
) : PreferencesDefaultsSource {
    override suspend fun defaults(): PreferencesDefaults = defaults
}
