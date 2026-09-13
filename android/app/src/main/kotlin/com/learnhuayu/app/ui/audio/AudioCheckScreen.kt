package com.learnhuayu.app.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnhuayu.app.R
import com.learnhuayu.core.ui.PlaybackButton
import com.learnhuayu.core.ui.RecordButton
import com.learnhuayu.core.ui.RecordButtonState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioCheckScreen(
    onBack: () -> Unit,
    viewModel: AudioCheckViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onPermissionResult,
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionStatusChecked(granted)
    }

    LaunchedEffect(uiState.permission) {
        if (uiState.permission == MicrophonePermission.Requesting) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onLeaveScreen() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audio_check_title)) },
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.audio_check_intro),
                style = MaterialTheme.typography.bodyLarge,
            )

            if (uiState.permission == MicrophonePermission.Denied) {
                Text(
                    text = stringResource(R.string.audio_check_permission_denied),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = viewModel::onRequestPermission) {
                    Text(text = stringResource(R.string.audio_check_permission_retry))
                }
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = stringResource(R.string.audio_check_error, message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text(
                text = stringResource(R.string.audio_check_reference_title),
                style = MaterialTheme.typography.titleMedium,
            )
            PlaybackButton(
                hasPlayed = uiState.referencePlayback.hasPlayed,
                onClick = viewModel::onPlayReferenceClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.processing && !uiState.isRecording,
            )

            Text(
                text = stringResource(R.string.audio_check_recording_title),
                style = MaterialTheme.typography.titleMedium,
            )
            RecordButton(
                state = uiState.recordButtonState(),
                onClick = viewModel::onRecordClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(
                    R.string.audio_check_input_level,
                    (uiState.level.coerceIn(0f, 1f) * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { uiState.level.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            PlaybackButton(
                hasPlayed = uiState.recordingPlayback.hasPlayed,
                onClick = viewModel::onPlayRecordingClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.hasRecordingPlayback,
            )
        }
    }
}

private fun AudioCheckUiState.recordButtonState(): RecordButtonState = when {
    processing -> RecordButtonState.Processing
    isRecording -> RecordButtonState.Recording
    else -> RecordButtonState.Idle
}
