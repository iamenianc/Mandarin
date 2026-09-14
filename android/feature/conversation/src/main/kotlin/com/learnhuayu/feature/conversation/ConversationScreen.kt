package com.learnhuayu.feature.conversation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnhuayu.core.ui.PinyinText
import com.learnhuayu.core.ui.RecordButton
import com.learnhuayu.core.ui.RecordButtonState
import kotlin.math.roundToInt

/**
 * The coaching conversation surface (FR-11, WF-2, `docs/03-design.md`): pick a starter
 * scenario, hear the coach opener, then trade spoken turns with the mic. Each coach reply
 * renders replyText with the optional gentle correction and next prompt; voiced replies
 * play when WF-3 succeeds and stay as text otherwise. UI text is English or pinyin only;
 * there is no hanzi (ADR 0011, ADR 0012).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationScreen(
    onBack: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel(),
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
        if (uiState.permission == ConversationPermission.Requesting) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onLeaveScreen() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Conversation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            if (!uiState.inSession) {
                ScenarioPicker(uiState = uiState, viewModel = viewModel)
            } else {
                ExchangeContent(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ScenarioPicker(uiState: ConversationUiState, viewModel: ConversationViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "intro") {
            Text(
                text = "Pick a scenario. The coach speaks first, then you answer with the mic.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        items(uiState.scenarios, key = { it.id }) { scenario ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = scenario.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = scenario.blurb,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { viewModel.onScenarioSelect(scenario.id) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.starting,
                    ) {
                        Text(text = "Start")
                    }
                }
            }
        }
        if (uiState.starting) {
            item(key = "loading") {
                CircularProgressIndicator()
            }
        }
    }
    uiState.errorMessage?.let { message ->
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ExchangeContent(uiState: ConversationUiState, viewModel: ConversationViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.selectedScenario?.let { scenario ->
            item(key = "scenario") {
                Text(text = scenario.label, style = MaterialTheme.typography.labelLarge)
            }
        }
        items(uiState.exchanges) { exchange ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = "Coach", style = MaterialTheme.typography.labelLarge)
                    PinyinText(
                        pinyin = exchange.reply.replyText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    exchange.reply.gentleCorrection?.let { correction ->
                        Text(
                            text = correction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    exchange.reply.nextPrompt?.let { prompt ->
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (!exchange.audioAvailable) {
                        Text(
                            text = "Audio unavailable for this reply.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (exchange.scripted) {
                        Text(
                            text = "Scripted reply.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (uiState.sending) {
            item(key = "loading") {
                CircularProgressIndicator()
            }
        }
        item(key = "controls") {
            RecordButton(
                state = when {
                    uiState.sending -> RecordButtonState.Processing
                    uiState.isRecording -> RecordButtonState.Recording
                    else -> RecordButtonState.Idle
                },
                onClick = viewModel::onRecordClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.sending,
            )
        }
        item(key = "level") {
            Text(
                text = "Input level: ${(uiState.level.coerceIn(0f, 1f) * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item(key = "meter") {
            LinearProgressIndicator(
                progress = { uiState.level.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "change") {
            TextButton(
                onClick = viewModel::onBackToScenarios,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.sending && !uiState.isRecording,
            ) {
                Text(text = "Change scenario")
            }
        }
    }
    uiState.errorMessage?.let { message ->
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
