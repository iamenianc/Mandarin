package com.learnhuayu.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnhuayu.app.R

/**
 * Settings surface (NFR-4): the recording-to-provider consent toggle with the accepted
 * consent version, the optional Hangul phonetic aid toggle, and one-tap data deletion
 * guarded by a confirmation dialog. Every visible word is English or pinyin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteCancelled,
            title = { Text(text = stringResource(R.string.settings_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.settings_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirmed) {
                    Text(text = stringResource(R.string.settings_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteCancelled) {
                    Text(text = stringResource(R.string.settings_delete_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ConsentSection(
                checked = uiState.recordingsToProviderConsent,
                consentVersion = uiState.consentVersion,
                onChange = viewModel::onRecordingsConsentChange,
            )
            HangulSection(
                checked = uiState.showHangul,
                onChange = viewModel::onShowHangulChange,
            )
            DeleteSection(
                deleting = uiState.deleting,
                outcome = uiState.deletionOutcome,
                onDeleteRequested = viewModel::onDeleteRequested,
            )
        }
    }
}

@Composable
private fun ConsentSection(
    checked: Boolean,
    consentVersion: String,
    onChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_consent_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_consent_explanation),
            style = MaterialTheme.typography.bodyMedium,
        )
        ToggleRow(
            label = stringResource(R.string.settings_consent_toggle),
            checked = checked,
            onChange = onChange,
        )
        Text(
            text = stringResource(R.string.settings_consent_version, consentVersion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HangulSection(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_hangul_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_hangul_explanation),
            style = MaterialTheme.typography.bodyMedium,
        )
        ToggleRow(
            label = stringResource(R.string.settings_hangul_toggle),
            checked = checked,
            onChange = onChange,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun DeleteSection(
    deleting: Boolean,
    outcome: DeletionOutcome?,
    onDeleteRequested: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_data_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_data_explanation),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onDeleteRequested,
            modifier = Modifier.fillMaxWidth(),
            enabled = !deleting,
        ) {
            Text(text = stringResource(R.string.settings_delete_action))
        }
        if (deleting) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.settings_delete_working),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        when (outcome) {
            DeletionOutcome.Success -> Text(
                text = stringResource(R.string.settings_delete_success),
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodyMedium,
            )

            DeletionOutcome.Failure -> Text(
                text = stringResource(R.string.settings_delete_failure),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )

            null -> Unit
        }
    }
}
