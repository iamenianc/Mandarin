package com.learnhuayu.feature.raymond

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnhuayu.core.ai.MandarinExample
import com.learnhuayu.core.ai.MandarinQaAnswer
import com.learnhuayu.core.ui.LargeTapTarget
import com.learnhuayu.core.ui.LevelMeter
import com.learnhuayu.core.ui.PinyinText
import com.learnhuayu.core.ui.RecordButton
import com.learnhuayu.core.ui.RecordButtonState
import kotlin.math.roundToInt

/**
 * Raymond's chat surface (FR-25, WF-7). Audio-first and large-target: the learner types or
 * speaks a question, reads the answer with pinyin examples, and taps a follow-up to keep
 * going. Pinyin is the only non-English notation and always carries tone numbers (ADR 0011).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RaymondScreen(
    onBack: () -> Unit,
    viewModel: RaymondViewModel = hiltViewModel(),
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
        if (uiState.permission == RaymondPermission.Requesting) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onLeaveScreen() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Raymond") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "intro") {
                    Text(
                        text = "Ask anything about Mandarin: pronunciation, tones, meaning, or usage.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                items(uiState.turns) { turn ->
                    TurnContent(
                        turn = turn,
                        enabled = !uiState.loading,
                        onFollowUp = viewModel::onFollowUpClick,
                    )
                }
                if (uiState.loading) {
                    item(key = "loading") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Getting Raymond\u2019s answer\u2026",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                if (!uiState.loading && uiState.turns.isEmpty() && uiState.errorMessage == null) {
                    item(key = "empty-hint") {
                        Text(
                            text = "No questions yet. Type above, or tap Record to ask by voice.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            AskControls(uiState = uiState, viewModel = viewModel)
        }
    }
}

@Composable
private fun TurnContent(
    turn: RaymondTurn,
    enabled: Boolean,
    onFollowUp: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "You", style = MaterialTheme.typography.labelLarge)
            Text(text = turn.question, style = MaterialTheme.typography.bodyLarge)
        }
    }

    when {
        turn.answer != null -> AnswerCard(
            answer = turn.answer,
            enabled = enabled,
            onFollowUp = onFollowUp,
        )

        turn.failed -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = RAYMOND_OFFLINE_MESSAGE,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Typing the question again works as soon as the connection returns.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        else -> Text(
            text = "Waiting for Raymond\u2019s answer\u2026",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnswerCard(
    answer: MandarinQaAnswer,
    enabled: Boolean,
    onFollowUp: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Raymond", style = MaterialTheme.typography.labelLarge)
            Text(text = answer.answerText, style = MaterialTheme.typography.bodyLarge)
            answer.examples.forEach { example -> ExampleContent(example = example) }
            if (answer.followUps.isNotEmpty()) {
                Text(text = "Ask next", style = MaterialTheme.typography.labelLarge)
                answer.followUps.forEach { suggestion ->
                    LargeTapTarget(
                        label = suggestion,
                        onClick = { onFollowUp(suggestion) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExampleContent(example: MandarinExample) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PinyinText(pinyin = example.pinyin, style = MaterialTheme.typography.titleMedium)
        Text(
            text = example.meaning,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AskControls(uiState: RaymondUiState, viewModel: RaymondViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (uiState.permission == RaymondPermission.Denied) {
            Text(
                text = "The microphone is blocked. Typing above always works. To use voice, allow the microphone in system Settings.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        OutlinedTextField(
            value = uiState.input,
            onValueChange = viewModel::onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Ask about Mandarin (English or pinyin)") },
            supportingText = {
                Text(
                    text = if (uiState.input.isBlank()) {
                        "Type a question to unlock Ask."
                    } else {
                        "Ask sends one question to Raymond. Voice input keeps the typed text."
                    },
                )
            },
            enabled = !uiState.loading && !uiState.isRecording,
        )
        Button(
            onClick = viewModel::onAskClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canAsk && !uiState.isRecording,
        ) {
            Text(text = "Ask")
        }
        Text(text = "Ask by voice", style = MaterialTheme.typography.bodyMedium)
        RecordButton(
            state = uiState.recordButtonState(),
            onClick = viewModel::onRecordClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.loading,
        )
        if (!uiState.isRecording && uiState.permission != RaymondPermission.Granted) {
            Text(
                text = "Tapping Record asks for the microphone. Typing above always works, even offline for drafts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "Input level: ${(uiState.level.coerceIn(0f, 1f) * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodyMedium,
        )
        LevelMeter(level = uiState.level, modifier = Modifier.fillMaxWidth())
    }
}

private fun RaymondUiState.recordButtonState(): RecordButtonState = when {
    isRecording -> RecordButtonState.Recording
    loading -> RecordButtonState.Processing
    else -> RecordButtonState.Idle
}
