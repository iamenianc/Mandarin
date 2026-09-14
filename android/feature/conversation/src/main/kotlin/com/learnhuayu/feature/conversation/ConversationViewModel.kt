package com.learnhuayu.feature.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnhuayu.core.ai.AudioClip
import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.ConversationState
import com.learnhuayu.core.ai.ConversationTurnReply
import com.learnhuayu.core.ai.ConversationTurnRequest
import com.learnhuayu.core.ai.ConversationTurnWorkflow
import com.learnhuayu.core.ai.SpeechSynthesisRequest
import com.learnhuayu.core.ai.SpeechSynthesisWorkflow
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.capture.AudioRecorder
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.audio.vad.VadEvent
import com.learnhuayu.core.audio.wav.WavCodec
import com.learnhuayu.core.data.repository.ConversationSessionRepository
import com.learnhuayu.core.data.repository.TurnRepository
import com.learnhuayu.core.model.ConversationSession
import com.learnhuayu.core.model.ConversationSpeaker
import com.learnhuayu.core.model.Turn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/** Friendly note shown when WF-2 fails and the turn falls back to a scripted reply. */
const val CONVERSATION_OFFLINE_MESSAGE = "Offline mode: using a scripted reply so practice keeps moving."

/** Friendly note shown when a recording cannot be used; the learner can record again. */
const val CONVERSATION_RECORDER_ERROR_MESSAGE = "The recording could not be used. Try recording again."

/** Microphone access state for the conversation recorder. */
enum class ConversationPermission {
    NotRequested,
    Requesting,
    Granted,
    Denied,
}

/** One coach turn: the WF-2 reply, whether its audio played, and whether it was scripted. */
data class ConversationExchange(
    val reply: ConversationTurnReply,
    val audioAvailable: Boolean = false,
    val scripted: Boolean = false,
)

/**
 * Drives the coaching conversation (FR-11, WF-2). The learner picks a starter scenario, the
 * coach opener is voiced, then each mic turn sends audio plus scenario, conversation state,
 * and target difficulty to [ConversationTurnWorkflow]. Replies render replyText with the
 * optional gentle correction and next prompt; WF-3 voices each reply with a silent text
 * fallback when synthesis is unavailable (including 501); any WF-2 failure falls back to a
 * scripted scenario line so the exchange keeps moving. Sessions and turns persist via
 * [ConversationSessionRepository] and [TurnRepository]; no new tables are introduced.
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val turnWorkflow: ConversationTurnWorkflow,
    private val speechWorkflow: SpeechSynthesisWorkflow,
    private val conversationAudioPlayer: ConversationAudioPlayer,
    private val sessionRepository: ConversationSessionRepository,
    private val turnRepository: TurnRepository,
    private val audioRecorder: AudioRecorder,
    private val wavCodec: WavCodec,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private var stopJob: Job? = null
    private var attemptPcm: PcmAudio? = null

    init {
        viewModelScope.launch {
            audioRecorder.state.collect { state -> onRecorderState(state) }
        }
        viewModelScope.launch {
            audioRecorder.level.collect { level -> _uiState.update { it.copy(level = level) } }
        }
        viewModelScope.launch {
            audioRecorder.vadEvents
                .filter { it is VadEvent.SpeechEnded }
                .collect { onSpeechEnded() }
        }
    }

    /** Starts a coaching session for [scenarioId]: persists the session and voices the opener. */
    fun onScenarioSelect(scenarioId: String) {
        val scenario = BundledConversation.scenario(scenarioId) ?: return
        if (_uiState.value.starting) return
        _uiState.update { it.copy(starting = true, errorMessage = null) }
        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            sessionRepository.upsert(
                ConversationSession(
                    id = sessionId,
                    lessonId = scenario.id,
                    startedAt = clock.instant(),
                ),
            )
            turnRepository.upsert(
                Turn(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    speaker = ConversationSpeaker.AI,
                    transcript = scenario.opener.replyText,
                    feedback = scenario.opener.gentleCorrection,
                ),
            )
            val audioAvailable = playReplyText(text = scenario.opener.replyText)
            _uiState.update {
                it.copy(
                    starting = false,
                    scenarioId = scenario.id,
                    sessionId = sessionId,
                    exchanges = listOf(
                        ConversationExchange(
                            reply = scenario.opener,
                            audioAvailable = audioAvailable,
                            scripted = false,
                        ),
                    ),
                    errorMessage = null,
                )
            }
        }
    }

    /** Leaves the session and returns to the scenario picker without losing persisted turns. */
    fun onBackToScenarios() {
        val sessionId = _uiState.value.sessionId
        if (sessionId != null) {
            viewModelScope.launch {
                endSession(sessionId)
            }
        }
        _uiState.update {
            it.copy(
                scenarioId = null,
                sessionId = null,
                exchanges = emptyList(),
                sending = false,
                errorMessage = null,
            )
        }
    }

    fun onPermissionStatusChecked(granted: Boolean) {
        _uiState.update { state ->
            when {
                granted -> state.copy(permission = ConversationPermission.Granted)
                state.permission == ConversationPermission.Granted -> state.copy(
                    permission = ConversationPermission.NotRequested,
                )
                else -> state
            }
        }
    }

    fun onRequestPermission() {
        if (_uiState.value.permission == ConversationPermission.Granted) return
        _uiState.update { it.copy(permission = ConversationPermission.Requesting) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                permission = if (granted) ConversationPermission.Granted else ConversationPermission.Denied,
            )
        }
    }

    /** Toggles the recorder: manual stop, or auto-stop on end of speech (VAD). */
    fun onRecordClick() {
        val state = _uiState.value
        when {
            state.sending || stopJob?.isActive == true -> Unit
            state.isRecording -> stopRecordingAndSend()
            state.permission != ConversationPermission.Granted -> onRequestPermission()
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

    private fun onSpeechEnded() {
        if (!_uiState.value.isRecording || _uiState.value.sending) return
        stopRecordingAndSend()
    }

    private fun stopRecordingAndSend() {
        if (stopJob?.isActive == true) return
        stopJob = viewModelScope.launch {
            attemptPcm = try {
                audioRecorder.stop()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = CONVERSATION_RECORDER_ERROR_MESSAGE) }
                null
            }
            val pcm = attemptPcm ?: return@launch
            sendAttempt(pcm)
        }
    }

    private fun sendAttempt(pcm: PcmAudio) {
        val state = _uiState.value
        val scenario = state.scenarioId?.let(BundledConversation::scenario) ?: return
        val sessionId = state.sessionId ?: return
        if (state.sending) return
        val clip = try {
            AudioClip(wavCodec.encode(pcm), AudioFormat.WAV)
        } catch (error: Exception) {
            _uiState.update { it.copy(errorMessage = CONVERSATION_RECORDER_ERROR_MESSAGE) }
            return
        }
        _uiState.update { it.copy(sending = true, errorMessage = null) }
        viewModelScope.launch {
            val conversationState = ConversationState(
                transcripts = state.exchanges.map { it.reply.replyText },
                corrections = state.exchanges.mapNotNull { it.reply.gentleCorrection },
            )
            val result = try {
                turnWorkflow.respond(
                    ConversationTurnRequest(
                        scenario = scenario.id,
                        audio = clip,
                        conversationState = conversationState,
                        targetDifficulty = scenario.targetDifficulty,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                null
            }
            applyReply(sessionId = sessionId, scenario = scenario, result = result)
        }
    }

    private suspend fun applyReply(
        sessionId: String,
        scenario: ConversationScenario,
        result: WorkflowResult<ConversationTurnReply>?,
    ) {
        val success = result as? WorkflowResult.Success
        val scripted = success == null
        val reply = if (success == null) {
            scenario.scriptedReply(_uiState.value.exchanges.size)
        } else {
            success.value
        }
        turnRepository.upsert(
            Turn(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                speaker = ConversationSpeaker.USER,
            ),
        )
        turnRepository.upsert(
            Turn(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                speaker = ConversationSpeaker.AI,
                transcript = reply.replyText,
                feedback = reply.gentleCorrection,
            ),
        )
        val existing = sessionRepository.byId(sessionId)
        if (existing != null) {
            sessionRepository.upsert(
                existing.copy(turnCount = _uiState.value.exchanges.size + 1),
            )
        }
        val audioAvailable = playReplyText(text = reply.replyText)
        _uiState.update { state ->
            state.copy(
                sending = false,
                exchanges = state.exchanges + ConversationExchange(
                    reply = reply,
                    audioAvailable = audioAvailable,
                    scripted = scripted,
                ),
                errorMessage = if (scripted) CONVERSATION_OFFLINE_MESSAGE else null,
            )
        }
    }

    /**
     * Voices [text] through WF-3 and the shared player. Returns false when synthesis is
     * unavailable; the caller keeps the text and skips audio only, including the 501
     * provider-not-configured case.
     */
    private suspend fun playReplyText(text: String): Boolean {
        val result = try {
            speechWorkflow.synthesize(SpeechSynthesisRequest.ReplyText(replyText = text))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            null
        }
        val success = result as? WorkflowResult.Success ?: return false
        return try {
            conversationAudioPlayer.playSpeech(success.value.bytes, success.value.contentType)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            false
        }
    }

    private suspend fun endSession(sessionId: String) {
        val existing = sessionRepository.byId(sessionId)
        if (existing != null) {
            sessionRepository.upsert(
                existing.copy(
                    endedAt = clock.instant(),
                    turnCount = _uiState.value.exchanges.size,
                ),
            )
        }
    }

    private fun onRecorderState(state: RecorderState) {
        _uiState.update {
            it.copy(
                recorderState = state,
                errorMessage = (state as? RecorderState.Failed)?.message ?: it.errorMessage,
            )
        }
    }
}

data class ConversationUiState(
    val scenarioId: String? = null,
    val sessionId: String? = null,
    val exchanges: List<ConversationExchange> = emptyList(),
    val starting: Boolean = false,
    val sending: Boolean = false,
    val errorMessage: String? = null,
    val permission: ConversationPermission = ConversationPermission.NotRequested,
    val recorderState: RecorderState = RecorderState.Idle,
    val level: Float = 0f,
) {
    val isRecording: Boolean
        get() = recorderState is RecorderState.Recording

    val scenarios: List<ConversationScenario>
        get() = BundledConversation.scenarios

    val selectedScenario: ConversationScenario?
        get() = scenarioId?.let(BundledConversation::scenario)

    val inSession: Boolean
        get() = scenarioId != null && sessionId != null
}
