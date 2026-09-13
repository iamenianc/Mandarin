package com.learnhuayu.core.data.prefs

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.data.content.ContentAssetException
import com.learnhuayu.core.data.content.FakeContentSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class PreferencesDefaultsSourceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun readsBundledDefaults() = runTest {
        val source =
            BundledPreferencesDefaultsSource(
                FakeContentSource(mapOf("config/defaults.json" to defaultsJson)),
                json,
            )

        val defaults = source.defaults()

        assertThat(defaults.dailyGoalMinutes).isNull()
        assertThat(defaults.sessionLengthMinutes).isNull()
        assertThat(defaults.showHangul).isFalse()
        assertThat(defaults.recordingsToProviderConsent).isFalse()
        assertThat(defaults.consentVersion).isEqualTo("0")
    }

    @Test
    fun readsConfiguredDefaults() = runTest {
        val source =
            BundledPreferencesDefaultsSource(
                FakeContentSource(mapOf("config/defaults.json" to configuredDefaultsJson)),
                json,
            )

        val defaults = source.defaults()

        assertThat(defaults.dailyGoalMinutes).isEqualTo(15)
        assertThat(defaults.sessionLengthMinutes).isEqualTo(10)
        assertThat(defaults.showHangul).isTrue()
        assertThat(defaults.recordingsToProviderConsent).isTrue()
        assertThat(defaults.consentVersion).isEqualTo("1")
    }

    @Test
    fun missingDefaultsFileIsReported() = runTest {
        val source = BundledPreferencesDefaultsSource(FakeContentSource(emptyMap()), json)

        val failure = runCatching { source.defaults() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ContentAssetException::class.java)
    }

    @Test
    fun malformedDefaultsFileIsReported() = runTest {
        val source =
            BundledPreferencesDefaultsSource(
                FakeContentSource(mapOf("config/defaults.json" to "{ not json")),
                json,
            )

        val failure = runCatching { source.defaults() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ContentAssetException::class.java)
    }
}
