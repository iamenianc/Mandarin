package com.learnhuayu.app.ui.session

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnhuayu.app.R
import com.learnhuayu.app.ui.audio.MicrophonePermission
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.ui.HangulAidText
import com.learnhuayu.core.ui.LargeTapTarget
import com.learnhuayu.core.ui.PinyinText
import com.learnhuayu.core.ui.PlaybackButton
import com.learnhuayu.core.ui.RecordButton
import com.learnhuayu.core.ui.RecordButtonState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    moduleId: String,
    kindValue: String,
    specId: String,
    onBack: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onPermissionResult,
    )

    LaunchedEffect(moduleId, kindValue, specId) {
        viewModel.load(moduleId, kindValue, specId)
    }

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

    val requestExit = { showExitDialog = true }
    BackHandler(enabled = uiState.hasProgress && !uiState.finished) { requestExit() }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = stringResource(R.string.session_leave_title)) },
            text = { Text(text = stringResource(R.string.session_leave_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onBack()
                    },
                ) {
                    Text(text = stringResource(R.string.session_leave_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(text = stringResource(R.string.session_leave_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.specTitle ?: stringResource(R.string.session_loading)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.hasProgress && !uiState.finished) requestExit() else onBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.loading -> LoadingState()
                uiState.notFound -> Text(
                    text = stringResource(R.string.session_not_found),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )

                uiState.finished -> FinishedState(uiState = uiState, onDone = onBack)
                uiState.currentItem != null -> SessionBody(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.session_loading),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun FinishedState(uiState: SessionUiState, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.session_finished_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.session_finished_body, uiState.completedCount),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            Text(text = stringResource(R.string.session_done))
        }
    }
}

@Composable
private fun SessionBody(uiState: SessionUiState, viewModel: SessionViewModel) {
    val item = uiState.currentItem ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.session_progress, uiState.index + 1, uiState.items.size),
            style = MaterialTheme.typography.labelLarge,
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = stringResource(R.string.session_error, message),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.session_reference_unavailable),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        uiState.recorderError?.let { message ->
            Text(
                text = stringResource(R.string.session_error, message),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        PlaybackButton(
            hasPlayed = uiState.referencePlayback.hasPlayed,
            onClick = viewModel::onPlayReferenceClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.processing && !uiState.isRecording,
        )

        when (uiState.mode) {
            DrillMode.LESSON -> LessonContent(uiState = uiState, item = item)
            DrillMode.HEAR_AND_NAME -> ChoiceContent(
                uiState = uiState,
                item = item,
                prompt = stringResource(R.string.session_hear_and_name_prompt),
                viewModel = viewModel,
            )

            DrillMode.LISTEN_AND_CHOOSE -> ChoiceContent(
                uiState = uiState,
                item = item,
                prompt = stringResource(R.string.session_listen_and_choose_prompt),
                viewModel = viewModel,
            )

            DrillMode.SPEAK_AND_REPEAT -> SpeakContent(uiState = uiState, item = item, viewModel = viewModel)

            DrillMode.SPEAK_AND_REPEAT_FEEDBACK -> SpeakAndRepeatFeedbackContent(
                uiState = uiState,
                item = item,
                viewModel = viewModel,
            )

            DrillMode.LISTEN_AND_ANSWER_SPOKEN -> SpokenAnswerContent(
                uiState = uiState,
                item = item,
                viewModel = viewModel,
            )

            null -> Unit
        }

        NavigationRow(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun LessonContent(uiState: SessionUiState, item: ContentItem) {
    Text(
        text = stringResource(R.string.session_lesson_prompt),
        style = MaterialTheme.typography.bodyLarge,
    )
    PinyinText(pinyin = item.pinyin)
    HangulAidText(hangul = item.hangul.takeIf { uiState.showHangul })
    Text(text = item.meaning, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun ChoiceContent(
    uiState: SessionUiState,
    item: ContentItem,
    prompt: String,
    viewModel: SessionViewModel,
) {
    Text(text = prompt, style = MaterialTheme.typography.bodyLarge)

    uiState.choices.forEach { choice ->
        LargeTapTarget(
            label = choice,
            onClick = { viewModel.onChoiceSelected(choice) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.answerRevealed,
        )
    }

    if (uiState.answerRevealed) {
        val correct = uiState.answerCorrect == true
        Text(
            text = if (correct) {
                stringResource(R.string.session_correct, uiState.correctAnswer.orEmpty())
            } else {
                stringResource(R.string.session_incorrect, uiState.correctAnswer.orEmpty())
            },
            color = if (correct) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
        )
        PinyinText(pinyin = item.pinyin, style = MaterialTheme.typography.titleLarge)
        HangulAidText(hangul = item.hangul.takeIf { uiState.showHangul })
        Text(text = item.meaning, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SpeakContent(uiState: SessionUiState, item: ContentItem, viewModel: SessionViewModel) {
    Text(
        text = stringResource(R.string.session_speak_and_repeat_prompt),
        style = MaterialTheme.typography.bodyLarge,
    )
    PinyinText(pinyin = item.pinyin)
    HangulAidText(hangul = item.hangul.takeIf { uiState.showHangul })
    Text(text = item.meaning, style = MaterialTheme.typography.bodyLarge)

    RecordControls(uiState = uiState, viewModel = viewModel)
}

/**
 * The spoken-answer drill: the reference plays, the learner records the pinyin they heard,
 * and the attempt is sent to WF-4 (ADR 0009). The same pinyin choices stay on screen and
 * tappable, so the drill completes when the worker is unreachable or the transcript does
 * not match. Pinyin is revealed only after the answer, as in the tap drills.
 */
@Composable
private fun SpokenAnswerContent(
    uiState: SessionUiState,
    item: ContentItem,
    viewModel: SessionViewModel,
) {
    Text(
        text = stringResource(R.string.session_listen_and_answer_prompt),
        style = MaterialTheme.typography.bodyLarge,
    )

    RecordControls(uiState = uiState, viewModel = viewModel)

    when {
        uiState.transcribing -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.session_spoken_answer_checking),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        uiState.attemptAudioRef != null && !uiState.answerRevealed -> Button(
            onClick = viewModel::onAnswerClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.processing && !uiState.isRecording,
        ) {
            Text(text = stringResource(R.string.session_spoken_answer_action))
        }
    }

    if (uiState.spokenAnswerFallback) {
        Text(
            text = stringResource(R.string.session_spoken_answer_fallback),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    ChoiceContent(
        uiState = uiState,
        item = item,
        prompt = stringResource(R.string.session_tap_fallback_prompt),
        viewModel = viewModel,
    )
}

@Composable
private fun RecordControls(uiState: SessionUiState, viewModel: SessionViewModel) {
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
        hasPlayed = uiState.attemptPlayback.hasPlayed,
        onClick = viewModel::onPlayAttemptClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = uiState.attemptAudioRef != null && !uiState.processing && !uiState.isRecording,
    )
}

@Composable
private fun SpeakAndRepeatFeedbackContent(
    uiState: SessionUiState,
    item: ContentItem,
    viewModel: SessionViewModel,
) {
    SpeakContent(uiState = uiState, item = item, viewModel = viewModel)
    if (uiState.attemptAudioRef != null) {
        FeedbackPanel(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun FeedbackPanel(uiState: SessionUiState, viewModel: SessionViewModel) {
    val busy = uiState.processing || uiState.isRecording

    when (val feedback = uiState.feedback) {
        FeedbackUiState.None -> Button(
            onClick = viewModel::onGetFeedbackClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        ) {
            Text(text = stringResource(R.string.session_get_feedback))
        }

        FeedbackUiState.Loading -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.session_feedback_loading),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        is FeedbackUiState.Available -> CoachingFeedback(feedback = feedback)
        FeedbackUiState.OfflineFallback -> OfflineFeedback()
    }

    if (uiState.feedback is FeedbackUiState.Available || uiState.feedback is FeedbackUiState.OfflineFallback) {
        Button(
            onClick = viewModel::onTryAgainClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        ) {
            Text(text = stringResource(R.string.session_try_again))
        }
    }
}

@Composable
private fun CoachingFeedback(feedback: FeedbackUiState.Available) {
    Text(
        text = stringResource(R.string.session_feedback_weakest_unit, feedback.feedback.weakestUnit),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.session_feedback_issue, feedback.feedback.issue),
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        text = stringResource(R.string.session_feedback_tip, feedback.feedback.tip),
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        text = feedback.feedback.encouragement,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.tertiary,
    )
    Text(
        text = stringResource(R.string.session_feedback_replay_hint, feedback.feedback.replayHint),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun OfflineFeedback() {
    Text(
        text = stringResource(R.string.session_feedback_offline_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.session_feedback_offline_note),
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        text = stringResource(R.string.session_feedback_offline_tip),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun NavigationRow(uiState: SessionUiState, viewModel: SessionViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (uiState.mode == DrillMode.LESSON) {
            Button(
                onClick = viewModel::onPrevious,
                modifier = Modifier.weight(1f),
                enabled = uiState.index > 0 && !uiState.isRecording && !uiState.processing,
            ) {
                Text(text = stringResource(R.string.session_previous))
            }
        }
        Button(
            onClick = viewModel::onNext,
            modifier = Modifier.weight(1f),
            enabled = uiState.canAdvance,
        ) {
            Text(
                text = if (uiState.isLastItem) {
                    stringResource(R.string.session_finish)
                } else {
                    stringResource(R.string.session_next)
                },
            )
        }
    }
}

private fun SessionUiState.recordButtonState(): RecordButtonState = when {
    processing -> RecordButtonState.Processing
    isRecording -> RecordButtonState.Recording
    else -> RecordButtonState.Idle
}
