package com.learnhuayu.feature.raymond

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnhuayu.core.ai.AudioClip
import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.MandarinQaAnswer
import com.learnhuayu.core.ai.MandarinQaRequest
import com.learnhuayu.core.ai.MandarinQaWorkflow
import com.learnhuayu.core.ai.QaExchange
import com.learnhuayu.core.ai.RaymondRole
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.capture.AudioRecorder
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.wav.WavCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Friendly, non-technical note shown when Raymond cannot reach the Worker (WF-7 fallback). */
const val RAYMOND_OFFLINE_MESSAGE = "Raymond needs a connection. Check your network and try again."

/** Friendly note shown when a recording cannot be used; the learner can always type instead. */
const val RAYMOND_RECORDER_ERROR_MESSAGE = "The recording could not be used. Type the question instead."

/** Readable label for a spoken question, used in the transcript and in Q&A history. */
const val RAYMOND_SPOKEN_QUESTION = "Spoken question"

/** Microphone access state for the optional ask-by-voice action. */
enum class RaymondPermission {
    NotRequested,
    Requesting,
    Granted,
    Denied,
}

/**
 * One exchange in the in-memory session transcript: the learner's question and Raymond's
 * answer, or a failed marker when the answer could not be fetched.
 */
data class RaymondTurn(
    val question: String,
    val answer: MandarinQaAnswer? = null,
    val failed: Boolean = false,
)

data class RaymondUiState(
    val input: String = "",
    val turns: List<RaymondTurn> = emptyList(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val permission: RaymondPermission = RaymondPermission.NotRequested,
    val recorderState: RecorderState = RecorderState.Idle,
    val level: Float = 0f,
) {
    val isRecording: Boolean
        get() = recorderState is RecorderState.Recording

    val canAsk: Boolean
        get() = !loading && input.isNotBlank()
}

/**
 * Drives the Raymond chat (WF-7): it sends the learner's typed or spoken question plus the
 * accumulated Q&A history to [MandarinQaWorkflow], keeps the transcript in memory for the
 * session, and never crashes - a workflow failure becomes a friendly offline note. No
 * learner level is sent yet (FR-25).
 */
@HiltViewModel
class RaymondViewModel @Inject constructor(
    private val workflow: MandarinQaWorkflow,
    private val audioRecorder: AudioRecorder,
    private val wavCodec: WavCodec,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RaymondUiState())
    val uiState: StateFlow<RaymondUiState> = _uiState.asStateFlow()

    private var stopJob: Job? = null

    init {
        viewModelScope.launch {
            audioRecorder.state.collect { state -> onRecorderState(state) }
        }
        viewModelScope.launch {
            audioRecorder.level.collect { level -> _uiState.update { it.copy(level = level) } }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun onAskClick() {
        val state = _uiState.value
        val question = state.input.trim()
        if (question.isEmpty() || state.loading) return
        ask(question = question, audio = null, displayQuestion = question, clearInput = true)
    }

    /** A tapped suggestion becomes the next question (`docs/03-design.md`). */
    fun onFollowUpClick(suggestion: String) {
        val trimmed = suggestion.trim()
        if (trimmed.isEmpty() || _uiState.value.loading) return
        _uiState.update { it.copy(input = trimmed) }
        ask(question = trimmed, audio = null, displayQuestion = trimmed, clearInput = true)
    }

    fun onPermissionStatusChecked(granted: Boolean) {
        _uiState.update { state ->
            when {
                granted -> state.copy(permission = RaymondPermission.Granted)
                state.permission == RaymondPermission.Granted -> state.copy(permission = RaymondPermission.NotRequested)
                else -> state
            }
        }
    }

    fun onRequestPermission() {
        if (_uiState.value.permission == RaymondPermission.Granted) return
        _uiState.update { it.copy(permission = RaymondPermission.Requesting) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                permission = if (granted) RaymondPermission.Granted else RaymondPermission.Denied,
            )
        }
    }

    fun onRecordClick() {
        val state = _uiState.value
        when {
            state.loading || stopJob?.isActive == true -> Unit
            state.isRecording -> stopRecordingAndAsk()
            state.permission != RaymondPermission.Granted -> onRequestPermission()
            else -> startRecording()
        }
    }

    fun onLeaveScreen() {
        if (!_uiState.value.isRecording) return
        viewModelScope.launch {
            runCatching { audioRecorder.stop() }
        }
    }

    private fun startRecording() {
        _uiState.update { it.copy(errorMessage = null) }
        audioRecorder.start()
    }

    private fun stopRecordingAndAsk() {
        if (stopJob?.isActive == true) return
        stopJob = viewModelScope.launch {
            val clip = try {
                AudioClip(wavCodec.encode(audioRecorder.stop()), AudioFormat.WAV)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = RAYMOND_RECORDER_ERROR_MESSAGE) }
                null
            }
            if (clip == null) return@launch
            val typed = _uiState.value.input.trim().ifBlank { null }
            ask(
                question = typed,
                audio = clip,
                displayQuestion = typed ?: RAYMOND_SPOKEN_QUESTION,
                clearInput = false,
            )
        }
    }

    private fun ask(
        question: String?,
        audio: AudioClip?,
        displayQuestion: String,
        clearInput: Boolean,
    ) {
        if (_uiState.value.loading) return
        val history = _uiState.value.history()
        val turnIndex = _uiState.value.turns.size
        _uiState.update {
            it.copy(
                input = if (clearInput) "" else it.input,
                turns = it.turns + RaymondTurn(question = displayQuestion),
                loading = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            val result = try {
                workflow.ask(
                    MandarinQaRequest(
                        question = question,
                        audio = audio,
                        history = history,
                        learnerLevel = null,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                null
            }
            applyResult(turnIndex, result)
        }
    }

    private fun applyResult(index: Int, result: WorkflowResult<MandarinQaAnswer>?) {
        _uiState.update { state ->
            val turns = state.turns.toMutableList()
            val turn = turns.getOrNull(index)
                ?: return@update state.copy(loading = false, errorMessage = RAYMOND_OFFLINE_MESSAGE)
            val success = result as? WorkflowResult.Success
            turns[index] = if (success == null) {
                turn.copy(failed = true)
            } else {
                turn.copy(answer = success.value, failed = false)
            }
            state.copy(
                turns = turns,
                loading = false,
                errorMessage = if (success == null) RAYMOND_OFFLINE_MESSAGE else null,
            )
        }
    }

    private fun onRecorderState(state: RecorderState) {
        _uiState.update {
            val failure = state as? RecorderState.Failed
            it.copy(
                recorderState = state,
                // Recorder failures carry technical detail, so surface the friendly
                // type-instead note rather than the raw message (WF-7 fallback intact).
                errorMessage = if (failure != null) RAYMOND_RECORDER_ERROR_MESSAGE else it.errorMessage,
            )
        }
    }

    private fun RaymondUiState.history(): List<QaExchange> {
        val exchanges = mutableListOf<QaExchange>()
        turns.forEach { turn ->
            exchanges += QaExchange(RaymondRole.USER, turn.question)
            turn.answer?.let { exchanges += QaExchange(RaymondRole.RAYMOND, it.answerText) }
        }
        return exchanges
    }
}
