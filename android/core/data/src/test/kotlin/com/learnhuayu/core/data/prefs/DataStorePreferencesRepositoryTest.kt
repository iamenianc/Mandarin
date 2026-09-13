package com.learnhuayu.core.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

class DataStorePreferencesRepositoryTest {
    private val defaults =
        PreferencesDefaults(
            dailyGoalMinutes = null,
            sessionLengthMinutes = null,
            showHangul = false,
            recordingsToProviderConsent = false,
            consentVersion = "0",
        )

    @Test
    fun fallsBackToDefaultsBeforeFirstWrite() = runTest {
        val repository = repository(backgroundScope)

        val stored = repository.preferences.first()

        assertThat(stored)
            .isEqualTo(
                UserPreferences(
                    dailyGoalMinutes = null,
                    sessionLengthMinutes = null,
                    showHangul = false,
                    recordingsToProviderConsent = false,
                    consentVersion = "0",
                ),
            )
    }

    @Test
    fun roundTripStoresAndClearsValues() = runTest {
        val repository = repository(backgroundScope)

        repository.setDailyGoalMinutes(15)
        repository.setSessionLengthMinutes(10)
        repository.setShowHangul(true)
        repository.setRecordingsToProviderConsent(true)
        repository.setConsentVersion("1")

        assertThat(repository.preferences.first())
            .isEqualTo(
                UserPreferences(
                    dailyGoalMinutes = 15,
                    sessionLengthMinutes = 10,
                    showHangul = true,
                    recordingsToProviderConsent = true,
                    consentVersion = "1",
                ),
            )

        repository.setDailyGoalMinutes(null)
        repository.setSessionLengthMinutes(null)

        assertThat(repository.preferences.first())
            .isEqualTo(
                UserPreferences(
                    dailyGoalMinutes = null,
                    sessionLengthMinutes = null,
                    showHangul = true,
                    recordingsToProviderConsent = true,
                    consentVersion = "1",
                ),
            )
    }

    @Test
    fun flowEmitsOnWrite() = runTest {
        val repository = repository(backgroundScope)

        repository.preferences.test {
            assertThat(awaitItem().showHangul).isFalse()

            repository.setShowHangul(true)

            assertThat(awaitItem().showHangul).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun repository(scope: CoroutineScope): DataStorePreferencesRepository = DataStorePreferencesRepository(
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempPreferencesFile() },
        ),
        FixedPreferencesDefaultsSource(defaults),
    )

    private fun tempPreferencesFile(): File = File.createTempFile("user_preferences", ".preferences_pb").apply { deleteOnExit() }
}
