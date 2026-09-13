package com.learnhuayu.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnhuayu.core.data.deletion.DataDeletionService
import com.learnhuayu.core.data.prefs.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The consent text version this build presents (`assets/legal/consent.md`). Granting
 * consent records it; revoking resets it to the "no consent" version used by defaults.
 */
const val CURRENT_CONSENT_VERSION = "1"

/** The stored consent version that means no consent is on record. */
const val NO_CONSENT_VERSION = "0"

/** How a delete attempt finished, so the screen can show an honest result. */
enum class DeletionOutcome {
    Success,
    Failure,
}

data class SettingsUiState(
    val recordingsToProviderConsent: Boolean = false,
    val consentVersion: String = NO_CONSENT_VERSION,
    val showHangul: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deleting: Boolean = false,
    val deletionOutcome: DeletionOutcome? = null,
)

/**
 * Drives the settings surface. Toggles persist immediately through
 * [PreferencesRepository]; deletion is gated behind an explicit confirmation and only
 * reports success after [DataDeletionService.deleteAll] returns (NFR-4).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val dataDeletionService: DataDeletionService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                _uiState.update {
                    it.copy(
                        recordingsToProviderConsent = preferences.recordingsToProviderConsent,
                        consentVersion = preferences.consentVersion,
                        showHangul = preferences.showHangul,
                    )
                }
            }
        }
    }

    fun onRecordingsConsentChange(granted: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRecordingsToProviderConsent(granted)
            preferencesRepository.setConsentVersion(
                if (granted) CURRENT_CONSENT_VERSION else NO_CONSENT_VERSION,
            )
        }
    }

    fun onShowHangulChange(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowHangul(enabled)
        }
    }

    /** Opens the confirmation dialog; deletion does not start until it is confirmed. */
    fun onDeleteRequested() {
        _uiState.update {
            it.copy(showDeleteConfirmation = true, deletionOutcome = null)
        }
    }

    fun onDeleteCancelled() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun onDeleteConfirmed() {
        if (_uiState.value.deleting) return
        _uiState.update {
            it.copy(
                showDeleteConfirmation = false,
                deleting = true,
                deletionOutcome = null,
            )
        }
        viewModelScope.launch {
            val outcome = try {
                dataDeletionService.deleteAll()
                DeletionOutcome.Success
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                DeletionOutcome.Failure
            }
            _uiState.update { it.copy(deleting = false, deletionOutcome = outcome) }
        }
    }
}
