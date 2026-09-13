package com.learnhuayu.feature.field

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnhuayu.core.model.DebriefEntry
import com.learnhuayu.core.model.DebriefKind
import com.learnhuayu.core.model.LocalPersona
import com.learnhuayu.core.model.ScriptTurn
import com.learnhuayu.core.ui.PinyinText
import com.learnhuayu.core.ui.RecordButton
import com.learnhuayu.core.ui.RecordButtonState
import kotlin.math.roundToInt

private val FIELD_THEMES = listOf(
    "market greeting",
    "ordering food",
    "asking directions",
    "small talk",
)

/**
 * The daily LAMP field loop surface (FR-30..FR-34, ADR 0013): one screen with three steps.
 * Rehearsal presents the day's script turn by turn with reference playback; the mission speaks
 * the exchange with five simulated locals in turn ("local 3 of 5"); the debrief logs
 * unfamiliar words and tone breakdowns as pinyin with tone numbers. UI text is English or
 * pinyin only; there is no hanzi (ADR 0011, ADR 0012).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FieldScreen(
    onBack: () -> Unit,
    viewModel: FieldViewModel = hiltViewModel(),
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
        if (uiState.permission == FieldPermission.Requesting) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onLeaveScreen() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Field") },
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
            when (uiState.step) {
                FieldStep.Rehearsal -> RehearsalContent(uiState = uiState, viewModel = viewModel)
                FieldStep.Mission -> MissionContent(uiState = uiState, viewModel = viewModel)
                FieldStep.Debrief -> DebriefContent(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun RehearsalContent(uiState: FieldUiState, viewModel: FieldViewModel) {
    var theme by remember { mutableStateOf(FIELD_THEMES.first()) }
    var expanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "intro") {
            Text(
                text = "Rehearse today's exchange, then use it with five locals.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (uiState.mission == null) {
            item(key = "theme") {
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = theme,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = "Theme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        FIELD_THEMES.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option) },
                                onClick = {
                                    theme = option
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            item(key = "generate") {
                Button(
                    onClick = { viewModel.startRehearsal(theme) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.generating,
                ) {
                    Text(text = "Get today's exchange")
                }
            }
            if (uiState.generating) {
                item(key = "loading") {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(uiState.mission.scriptTurns, key = { it.pinyin }) { turn ->
                ScriptTurnCard(turn = turn, onPlay = { viewModel.onPlayScriptTurn(turn) })
            }
            item(key = "start") {
                Button(
                    onClick = viewModel::onStartMission,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canStartMission,
                ) {
                    Text(text = "Start the mission")
                }
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
private fun ScriptTurnCard(turn: ScriptTurn, onPlay: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PinyinText(pinyin = turn.pinyin, style = MaterialTheme.typography.titleLarge)
            Text(
                text = turn.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onPlay) {
                Text(text = "Play reference")
            }
        }
    }
}

@Composable
private fun MissionContent(uiState: FieldUiState, viewModel: FieldViewModel) {
    val local = uiState.currentLocal
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.localProgressLabel?.let { label ->
            item(key = "progress") {
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (local != null) {
            item(key = "local") {
                LocalCard(local = local)
            }
        }
        items(uiState.exchanges) { exchange ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = "Local", style = MaterialTheme.typography.labelLarge)
                    PinyinText(
                        pinyin = exchange.reply.replyText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (!exchange.audioAvailable) {
                        Text(
                            text = "Audio unavailable for this reply.",
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
        if (uiState.exchanges.isNotEmpty() && !uiState.sending && !uiState.isRecording) {
            item(key = "next") {
                Button(
                    onClick = viewModel::onNextLocal,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = if (uiState.isLastLocal) "Finish and debrief" else "Next local")
                }
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
private fun LocalCard(local: LocalPersona) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = local.label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = local.settingRole,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = local.personality,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DebriefContent(uiState: FieldUiState, viewModel: FieldViewModel) {
    var pinyin by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(DebriefKind.UNFAMILIAR_WORD) }
    var kindExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "intro") {
            Text(
                text = "Log the words you did not catch and the tones that broke down, in pinyin with tone numbers.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (!uiState.finished) {
            item(key = "form") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinyin,
                        onValueChange = { pinyin = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Pinyin with tone numbers (for example ni3 hao3)") },
                    )
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = kindExpanded,
                        onExpandedChange = { kindExpanded = !kindExpanded },
                    ) {
                        OutlinedTextField(
                            value = if (kind == DebriefKind.UNFAMILIAR_WORD) "Unfamiliar word" else "Tone breakdown",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(text = "Kind") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = kindExpanded,
                            onDismissRequest = { kindExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = "Unfamiliar word") },
                                onClick = {
                                    kind = DebriefKind.UNFAMILIAR_WORD
                                    kindExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Tone breakdown") },
                                onClick = {
                                    kind = DebriefKind.TONE_BREAKDOWN
                                    kindExpanded = false
                                },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Note (optional, English or pinyin)") },
                    )
                    Button(
                        onClick = {
                            viewModel.onAddDebriefEntry(pinyin, kind, note)
                            pinyin = ""
                            note = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = pinyin.isNotBlank(),
                    ) {
                        Text(text = "Log entry")
                    }
                }
            }
        }
        items(uiState.debriefEntries, key = { it.id }) { entry ->
            DebriefEntryCard(entry = entry)
        }
        if (uiState.finished) {
            item(key = "done") {
                Text(
                    text = "Loop complete. These entries become themes for later missions.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            item(key = "back") {
                TextButton(onClick = viewModel::onBackToMission) {
                    Text(text = "Back to mission")
                }
            }
        } else {
            item(key = "finish") {
                Button(
                    onClick = viewModel::onFinishDebrief,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canFinishDebrief,
                ) {
                    Text(text = "Finish the loop")
                }
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
private fun DebriefEntryCard(entry: DebriefEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PinyinText(pinyin = entry.pinyin, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (entry.kind == DebriefKind.UNFAMILIAR_WORD) "Unfamiliar word" else "Tone breakdown",
                style = MaterialTheme.typography.labelLarge,
            )
            entry.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
