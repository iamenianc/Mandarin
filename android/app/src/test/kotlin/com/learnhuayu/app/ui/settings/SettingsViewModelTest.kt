package com.learnhuayu.app.ui.settings

import com.learnhuayu.app.ui.audio.MainDispatcherRule
import com.learnhuayu.app.ui.session.FakePreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakePreferencesRepository()
    private val deletion = FakeDataDeletionService()

    private fun viewModel(): SettingsViewModel = SettingsViewModel(preferences, deletion)

    @Test
    fun `consent toggle persists and records the consent version`() = runTest {
        val viewModel = viewModel()

        viewModel.onRecordingsConsentChange(true)

        assertTrue(preferences.preferences.first().recordingsToProviderConsent)
        assertEquals(CURRENT_CONSENT_VERSION, preferences.preferences.first().consentVersion)
        assertTrue(viewModel.uiState.value.recordingsToProviderConsent)
        assertEquals(CURRENT_CONSENT_VERSION, viewModel.uiState.value.consentVersion)
    }

    @Test
    fun `withdrawing consent persists the withdrawal`() = runTest {
        preferences.setRecordingsToProviderConsent(true)
        preferences.setConsentVersion(CURRENT_CONSENT_VERSION)
        val viewModel = viewModel()

        viewModel.onRecordingsConsentChange(false)

        assertFalse(preferences.preferences.first().recordingsToProviderConsent)
        assertEquals(NO_CONSENT_VERSION, preferences.preferences.first().consentVersion)
        assertFalse(viewModel.uiState.value.recordingsToProviderConsent)
    }

    @Test
    fun `hangul toggle persists`() = runTest {
        val viewModel = viewModel()

        viewModel.onShowHangulChange(true)

        assertTrue(preferences.preferences.first().showHangul)
        assertTrue(viewModel.uiState.value.showHangul)
    }

    @Test
    fun `delete waits for confirmation before calling the service`() = runTest {
        val viewModel = viewModel()

        viewModel.onDeleteRequested()

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(0, deletion.deleteCount)
    }

    @Test
    fun `cancelled delete leaves data untouched`() = runTest {
        val viewModel = viewModel()

        viewModel.onDeleteRequested()
        viewModel.onDeleteCancelled()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(0, deletion.deleteCount)
    }

    @Test
    fun `confirmed delete clears data and returns toggles to their defaults`() = runTest {
        preferences.setRecordingsToProviderConsent(true)
        preferences.setShowHangul(true)
        preferences.setConsentVersion(CURRENT_CONSENT_VERSION)
        deletion.onDelete = {
            preferences.setRecordingsToProviderConsent(false)
            preferences.setShowHangul(false)
            preferences.setConsentVersion(NO_CONSENT_VERSION)
        }
        val viewModel = viewModel()

        viewModel.onDeleteRequested()
        viewModel.onDeleteConfirmed()

        assertEquals(1, deletion.deleteCount)
        assertEquals(DeletionOutcome.Success, viewModel.uiState.value.deletionOutcome)
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertFalse(viewModel.uiState.value.deleting)
        assertFalse(viewModel.uiState.value.recordingsToProviderConsent)
        assertFalse(viewModel.uiState.value.showHangul)
    }

    @Test
    fun `failed delete reports failure and does not pretend success`() = runTest {
        preferences.setRecordingsToProviderConsent(true)
        preferences.setShowHangul(true)
        deletion.failure = IllegalStateException("storage unavailable")
        val viewModel = viewModel()

        viewModel.onDeleteRequested()
        viewModel.onDeleteConfirmed()

        assertEquals(1, deletion.deleteCount)
        assertEquals(DeletionOutcome.Failure, viewModel.uiState.value.deletionOutcome)
        assertFalse(viewModel.uiState.value.deleting)
        assertTrue(viewModel.uiState.value.recordingsToProviderConsent)
        assertTrue(viewModel.uiState.value.showHangul)
    }
}
