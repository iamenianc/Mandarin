package com.learnhuayu.feature.field

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnhuayu.core.ai.AudioClip
import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.ConversationState
import com.learnhuayu.core.ai.FieldMissionGenerationRequest
import com.learnhuayu.core.ai.FieldMissionGenerationWorkflow
import com.learnhuayu.core.ai.GeneratedMission
import com.learnhuayu.core.ai.LocalTurnMission
import com.learnhuayu.core.ai.LocalTurnReply
import com.learnhuayu.core.ai.LocalTurnRequest
import com.learnhuayu.core.ai.LocalTurnWorkflow
import com.learnhuayu.core.ai.MissionLocal
import com.learnhuayu.core.ai.SpeechSynthesisRequest
import com.learnhuayu.core.ai.SpeechSynthesisWorkflow
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.capture.AudioRecorder
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.audio.vad.VadEvent
import com.learnhuayu.core.audio.wav.WavCodec
import com.learnhuayu.core.data.repository.DebriefEntryRepository
import com.learnhuayu.core.data.repository.FieldMissionRepository
import com.learnhuayu.core.data.repository.LocalPersonaRepository
import com.learnhuayu.core.data.repository.MissionSessionRepository
import com.learnhuayu.core.data.repository.MissionTurnRepository
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.DebriefEntry
import com.learnhuayu.core.model.DebriefKind
import com.learnhuayu.core.model.FieldMission
import com.learnhuayu.core.model.LocalPersona
import com.learnhuayu.core.model.MissionSession
import com.learnhuayu.core.model.MissionSpeaker
import com.learnhuayu.core.model.MissionTurn
import com.learnhuayu.core.model.ScriptTurn
import com.learnhuayu.feature.field.BundledFieldMission.toMissionLocal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Friendly note shown when generation fails and the loop falls back to the bundled mission. */
const val FIELD_OFFLINE_MESSAGE = "Offline mode: using the bundled market mission."

/** Friendly note shown when a recording cannot be used; the learner can record again. */
const val FIELD_RECORDER_ERROR_MESSAGE = "The recording could not be used. Try recording again."

/** The loop's three steps, in order (ADR 0013). */
enum class FieldStep {
    Rehearsal,
    Mission,
    Debrief,
}

/** Microphone access state for the field mission recorder. */
enum class FieldPermission {
    NotRequested,
    Requesting,
    Granted,
    Denied,
}

/** One learner turn with the current local: the local reply plus the local's signal. */
data class FieldExchange(
    val reply: LocalTurnReply,
    val audioAvailable: Boolean = false,
)

/**
 * Drives the daily LAMP field loop (FR-30..FR-34, ADR 0013). Step 1 rehearses the day's
 * script, step 2 speaks it with five simulated locals in turn, step 3 logs debrief entries.
 *
 * Data flow: WF-9 generates the script and five locals; the mission and personas persist via
 * [FieldMissionRepository] and [LocalPersonaRepository]. In the mission the recorder captures
 * one attempt per local, WF-10 answers in character, and WF-3 voices the reply when it
 * succeeds. Sessions and turns persist via [MissionSessionRepository] and
 * [MissionTurnRepository]; debrief entries persist via [DebriefEntryRepository] tied to the
 * mission session. Every failure path keeps the loop moving: generation failure falls back to
 * the bundled mission, a TTS failure just skips audio, and a WF-10 failure shows the reply
 * text as unavailable without losing the session.
 */
@HiltViewModel
class FieldViewModel @Inject constructor(
    private val generationWorkflow: FieldMissionGenerationWorkflow,
    private val turnWorkflow: LocalTurnWorkflow,
    private val speechWorkflow: SpeechSynthesisWorkflow,
    private val fieldAudioPlayer: FieldAudioPlayer,
    private val missionRepository: FieldMissionRepository,
    private val personaRepository: LocalPersonaRepository,
    private val sessionRepository: MissionSessionRepository,
    private val turnRepository: MissionTurnRepository,
    private val debriefRepository: DebriefEntryRepository,
    private val audioRecorder: AudioRecorder,
    private val wavCodec: WavCodec,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FieldUiState())
    val uiState: StateFlow<FieldUiState> = _uiState.asStateFlow()

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

    /** Starts step 1: generate the mission for [theme], persisting whatever is used. */
    fun startRehearsal(theme: String) {
        if (_uiState.value.generating) return
        _uiState.update { it.copy(generating = true, errorMessage = null) }
        viewModelScope.launch {
            val result = try {
                generationWorkflow.generate(FieldMissionGenerationRequest(theme = theme))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                null
            }
            applyMission(result, theme)
        }
    }

    /** Plays the reference audio for one rehearsal script line; TTS failure just skips audio. */
    fun onPlayScriptTurn(turn: ScriptTurn) {
        viewModelScope.launch {
            playReplyText(text = turn.pinyin, voice = null)
        }
    }

    /** Moves from rehearsal to the mission once a mission is ready. */
    fun onStartMission() {
        val state = _uiState.value
        if (state.mission == null || state.locals.isEmpty()) return
        _uiState.update { it.copy(step = FieldStep.Mission) }
        startSessionForCurrentLocal()
    }

    fun onPermissionStatusChecked(granted: Boolean) {
        _uiState.update { state ->
            when {
                granted -> state.copy(permission = FieldPermission.Granted)
                state.permission == FieldPermission.Granted -> state.copy(permission = FieldPermission.NotRequested)
                else -> state
            }
        }
    }

    fun onRequestPermission() {
        if (_uiState.value.permission == FieldPermission.Granted) return
        _uiState.update { it.copy(permission = FieldPermission.Requesting) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                permission = if (granted) FieldPermission.Granted else FieldPermission.Denied,
            )
        }
    }

    /** Toggles the mission recorder: manual stop, or auto-stop on end of speech (VAD). */
    fun onRecordClick() {
        val state = _uiState.value
        when {
            state.sending || stopJob?.isActive == true -> Unit
            state.isRecording -> stopRecordingAndSend()
            state.permission != FieldPermission.Granted -> onRequestPermission()
            else -> startRecording()
        }
    }

    fun onLeaveScreen() {
        if (!_uiState.value.isRecording) return
        viewModelScope.launch {
            runCatching { audioRecorder.stop() }
        }
    }

    /** Moves to the next local after a turn completes; the fifth local opens the debrief. */
    fun onNextLocal() {
        val state = _uiState.value
        if (state.localIndex >= state.locals.lastIndex) {
            endSessionThen { openDebriefAsync() }
        } else {
            endSessionThen {
                _uiState.update { it.copy(localIndex = it.localIndex + 1) }
                startSessionForCurrentLocal()
            }
        }
    }

    /** Moves back to the mission from the debrief without losing the log. */
    fun onBackToMission() {
        _uiState.update { it.copy(step = FieldStep.Mission) }
    }

    /** Persists one debrief entry tied to the mission's session (FR-33, pinyin only). */
    fun onAddDebriefEntry(pinyin: String, kind: DebriefKind, note: String?) {
        val state = _uiState.value
        val mission = state.mission ?: return
        val trimmed = pinyin.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            debriefRepository.upsert(
                DebriefEntry(
                    id = UUID.randomUUID().toString(),
                    missionId = mission.id,
                    pinyin = trimmed,
                    kind = kind,
                    note = note?.trim().orEmpty().ifEmpty { null },
                    createdAt = clock.instant(),
                ),
            )
            refreshDebriefEntries(mission.id)
        }
    }

    /** Finishes the loop once at least one entry is logged; keeps the log visible. */
    fun onFinishDebrief() {
        if (_uiState.value.debriefEntries.isEmpty()) return
        _uiState.update { it.copy(finished = true) }
    }

    private suspend fun applyMission(result: WorkflowResult<GeneratedMission>?, theme: String) {
        val success = result as? WorkflowResult.Success
        if (success == null) {
            val fallback = BundledFieldMission.mission(date = LocalDate.now(clock))
            missionRepository.upsert(fallback)
            BundledFieldMission.locals.forEach { personaRepository.upsert(it) }
            _uiState.update {
                it.copy(
                    generating = false,
                    mission = fallback,
                    locals = BundledFieldMission.locals,
                    errorMessage = FIELD_OFFLINE_MESSAGE,
                )
            }
            return
        }
        val mission = FieldMission(
            id = UUID.randomUUID().toString(),
            date = LocalDate.now(clock),
            theme = theme,
            scriptTurns = success.value.script,
            source = ContentSource.GENERATED,
            localPersonaIds = success.value.locals.mapIndexed { index, local ->
                "$theme-local-$index"
            },
        )
        missionRepository.upsert(mission)
        val personas = success.value.locals.mapIndexed { index, local ->
            LocalPersona(
                id = "$theme-local-$index",
                label = local.label,
                settingRole = local.settingRole,
                personality = local.personality,
                voiceProfile = local.voiceProfile,
                pace = local.pace,
            ).also { personaRepository.upsert(it) }
        }
        _uiState.update {
            it.copy(
                generating = false,
                mission = mission,
                locals = personas,
                errorMessage = null,
            )
        }
    }

    private fun startSessionForCurrentLocal() {
        val state = _uiState.value
        val mission = state.mission ?: return
        val persona = state.locals.getOrNull(state.localIndex) ?: return
        val sessionId = UUID.randomUUID().toString()
        viewModelScope.launch {
            sessionRepository.upsert(
                MissionSession(
                    id = sessionId,
                    missionId = mission.id,
                    personaId = persona.id,
                    startedAt = clock.instant(),
                ),
            )
            _uiState.update {
                it.copy(
                    sessionId = sessionId,
                    exchanges = emptyList(),
                    sending = false,
                    errorMessage = null,
                )
            }
        }
    }

    private fun endSessionThen(after: () -> Unit) {
        val sessionId = _uiState.value.sessionId
        if (sessionId == null) {
            after()
            return
        }
        viewModelScope.launch {
            val existing = sessionRepository.byId(sessionId)
            if (existing != null) {
                sessionRepository.upsert(
                    existing.copy(
                        endedAt = clock.instant(),
                        turnCount = _uiState.value.exchanges.size,
                    ),
                )
            }
            _uiState.update { it.copy(sessionId = null) }
            after()
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
                _uiState.update { it.copy(errorMessage = FIELD_RECORDER_ERROR_MESSAGE) }
                null
            }
            val pcm = attemptPcm ?: return@launch
            sendAttempt(pcm)
        }
    }

    private fun sendAttempt(pcm: PcmAudio) {
        val state = _uiState.value
        val mission = state.mission ?: return
        val persona = state.locals.getOrNull(state.localIndex) ?: return
        val sessionId = state.sessionId ?: return
        if (state.sending) return
        val clip = try {
            AudioClip(wavCodec.encode(pcm), AudioFormat.WAV)
        } catch (error: Exception) {
            _uiState.update { it.copy(errorMessage = FIELD_RECORDER_ERROR_MESSAGE) }
            return
        }
        _uiState.update { it.copy(sending = true, errorMessage = null) }
        viewModelScope.launch {
            val conversationState = ConversationState(
                transcripts = state.exchanges.map { it.reply.replyText },
            )
            val result = try {
                turnWorkflow.respond(
                    LocalTurnRequest(
                        persona = persona.toMissionLocal(),
                        mission = LocalTurnMission(script = mission.scriptTurns),
                        audio = clip,
                        conversationState = conversationState,
                        targetDifficulty = null,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                null
            }
            applyReply(sessionId = sessionId, persona = persona, result = result)
        }
    }

    private suspend fun applyReply(
        sessionId: String,
        persona: LocalPersona,
        result: WorkflowResult<LocalTurnReply>?,
    ) {
        val success = result as? WorkflowResult.Success
        if (success == null) {
            _uiState.update { it.copy(sending = false, errorMessage = FIELD_OFFLINE_MESSAGE) }
            return
        }
        turnRepository.upsert(
            MissionTurn(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                speaker = MissionSpeaker.LEARNER,
            ),
        )
        turnRepository.upsert(
            MissionTurn(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                speaker = MissionSpeaker.LOCAL,
                transcript = success.value.replyText,
            ),
        )
        val audioAvailable = playReplyText(text = success.value.replyText, voice = persona.voiceProfile)
        _uiState.update { state ->
            state.copy(
                sending = false,
                exchanges = state.exchanges + FieldExchange(
                    reply = success.value,
                    audioAvailable = audioAvailable,
                ),
                errorMessage = null,
            )
        }
    }

    /**
     * Voices [text] through WF-3 and the shared player. Returns false when TTS is
     * unavailable; the caller keeps the text and skips audio only.
     */
    private suspend fun playReplyText(text: String, voice: String?): Boolean {
        val request = SpeechSynthesisRequest.ReplyText(replyText = text, voice = voice)
        val result = try {
            speechWorkflow.synthesize(request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            null
        }
        val success = result as? WorkflowResult.Success ?: return false
        return try {
            fieldAudioPlayer.playSpeech(success.value.bytes, success.value.contentType)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            false
        }
    }

    private fun LocalPersona.toMissionLocal(): MissionLocal = MissionLocal(
        label = label,
        settingRole = settingRole,
        personality = personality,
        voiceProfile = voiceProfile,
        pace = pace,
    )

    private fun openDebriefAsync() {
        viewModelScope.launch {
            val mission = _uiState.value.mission ?: return@launch
            refreshDebriefEntries(mission.id)
            _uiState.update { it.copy(step = FieldStep.Debrief) }
        }
    }

    private suspend fun refreshDebriefEntries(missionId: String) {
        val entries = debriefRepository.byMission(missionId).first()
        _uiState.update { it.copy(debriefEntries = entries) }
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

data class FieldUiState(
    val step: FieldStep = FieldStep.Rehearsal,
    val generating: Boolean = false,
    val mission: FieldMission? = null,
    val locals: List<LocalPersona> = emptyList(),
    val localIndex: Int = 0,
    val sessionId: String? = null,
    val exchanges: List<FieldExchange> = emptyList(),
    val sending: Boolean = false,
    val debriefEntries: List<DebriefEntry> = emptyList(),
    val finished: Boolean = false,
    val errorMessage: String? = null,
    val permission: FieldPermission = FieldPermission.NotRequested,
    val recorderState: RecorderState = RecorderState.Idle,
    val level: Float = 0f,
) {
    val isRecording: Boolean
        get() = recorderState is RecorderState.Recording

    val currentLocal: LocalPersona?
        get() = locals.getOrNull(localIndex)

    /** Design progress label: locals are 1-based ("local 3 of 5", `docs/03-design.md`). */
    val localProgressLabel: String?
        get() = if (locals.isEmpty()) null else "local ${localIndex + 1} of ${locals.size}"

    val canStartMission: Boolean
        get() = step == FieldStep.Rehearsal && mission != null && locals.isNotEmpty() && !generating

    val isLastLocal: Boolean
        get() = locals.isNotEmpty() && localIndex >= locals.lastIndex

    val canFinishDebrief: Boolean
        get() = step == FieldStep.Debrief && debriefEntries.isNotEmpty() && !finished
}
