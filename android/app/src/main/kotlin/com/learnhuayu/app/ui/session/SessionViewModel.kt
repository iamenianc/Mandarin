package com.learnhuayu.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnhuayu.app.audio.RecordingStore
import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.app.ui.audio.MicrophonePermission
import com.learnhuayu.core.ai.AudioClip
import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.PronunciationFeedback
import com.learnhuayu.core.ai.PronunciationFeedbackRequest
import com.learnhuayu.core.ai.PronunciationFeedbackWorkflow
import com.learnhuayu.core.ai.ResponseTranscription
import com.learnhuayu.core.ai.ResponseTranscriptionRequest
import com.learnhuayu.core.ai.ResponseTranscriptionWorkflow
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.capture.AudioRecorder
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.audio.playback.AudioPlayer
import com.learnhuayu.core.audio.playback.AudioSource
import com.learnhuayu.core.audio.playback.PlaybackState
import com.learnhuayu.core.audio.playback.PlaybackStatus
import com.learnhuayu.core.audio.wav.WavCodec
import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.data.prefs.PreferencesRepository
import com.learnhuayu.core.data.repository.AttemptRepository
import com.learnhuayu.core.data.repository.ProgressRepository
import com.learnhuayu.core.model.Attempt
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.Progress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.util.Locale
import javax.inject.Inject

const val SESSION_ATTEMPT_LABEL = "attempt"

data class SessionPlayback(
    val state: PlaybackState = PlaybackState(),
    val hasPlayed: Boolean = false,
)

/**
 * The WF-1 step of a speak-and-repeat drill. [None] before an attempt, then [Loading] while
 * the Worker answers, [Available] with the coaching, or [OfflineFallback] when the reference
 * clip is missing or the Worker is unreachable (ADR 0005, ADR 0014).
 */
sealed interface FeedbackUiState {
    data object None : FeedbackUiState

    data object Loading : FeedbackUiState

    data class Available(val feedback: PronunciationFeedback) : FeedbackUiState

    data object OfflineFallback : FeedbackUiState
}

data class SessionUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val moduleTitle: String? = null,
    val specTitle: String? = null,
    val mode: DrillMode? = null,
    val items: List<ContentItem> = emptyList(),
    val index: Int = 0,
    val completedCount: Int = 0,
    val finished: Boolean = false,
    val showHangul: Boolean = false,
    val permission: MicrophonePermission = MicrophonePermission.NotRequested,
    val referencePlayback: SessionPlayback = SessionPlayback(),
    val choices: List<String> = emptyList(),
    val selectedChoice: String? = null,
    val answerRevealed: Boolean = false,
    val answerCorrect: Boolean? = null,
    val correctAnswer: String? = null,
    val recorderState: RecorderState = RecorderState.Idle,
    val level: Float = 0f,
    val processing: Boolean = false,
    val transcribing: Boolean = false,
    val spokenAnswerFallback: Boolean = false,
    val attemptAudioRef: String? = null,
    val attemptPlayback: SessionPlayback = SessionPlayback(),
    val feedback: FeedbackUiState = FeedbackUiState.None,
    val errorMessage: String? = null,
    val recorderError: String? = null,
) {
    val currentItem: ContentItem?
        get() = items.getOrNull(index)

    val isLastItem: Boolean
        get() = items.isNotEmpty() && index == items.lastIndex

    val isRecording: Boolean
        get() = recorderState is RecorderState.Recording

    val isChoiceMode: Boolean
        get() = mode == DrillMode.HEAR_AND_NAME ||
            mode == DrillMode.LISTEN_AND_CHOOSE ||
            mode == DrillMode.LISTEN_AND_ANSWER_SPOKEN

    /** The spoken-answer drill: the learner speaks, but the tap choices remain the fallback. */
    val isSpokenAnswerMode: Boolean
        get() = mode == DrillMode.LISTEN_AND_ANSWER_SPOKEN

    val isFeedbackMode: Boolean
        get() = mode == DrillMode.SPEAK_AND_REPEAT_FEEDBACK

    val hasProgress: Boolean
        get() = index > 0 || answerRevealed || attemptAudioRef != null

    val canAdvance: Boolean
        get() {
            if (isRecording || processing || items.isEmpty()) return false
            return when (mode) {
                DrillMode.LESSON,
                DrillMode.SPEAK_AND_REPEAT,
                DrillMode.SPEAK_AND_REPEAT_FEEDBACK,
                -> true

                DrillMode.HEAR_AND_NAME,
                DrillMode.LISTEN_AND_CHOOSE,
                DrillMode.LISTEN_AND_ANSWER_SPOKEN,
                -> answerRevealed

                null -> false
            }
        }
}

/**
 * Runs one lesson or practice spec end to end against the bundled corpus. The mode decides
 * the interaction (browse, hear-and-name, listen-and-choose, speak-and-repeat, spoken answer);
 * the item stream and reference audio are shared across modes (ADR 0007). Progress is written
 * as the learner moves past each item; a speaking attempt also records an [Attempt].
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val registry: ModuleRegistry,
    private val contentRepository: BundledContentRepository,
    private val audioPlayer: AudioPlayer,
    private val audioRecorder: AudioRecorder,
    private val wavCodec: WavCodec,
    private val recordingStore: RecordingStore,
    private val progressRepository: ProgressRepository,
    private val attemptRepository: AttemptRepository,
    private val preferencesRepository: PreferencesRepository,
    private val feedbackWorkflow: PronunciationFeedbackWorkflow,
    private val responseWorkflow: ResponseTranscriptionWorkflow,
    private val referenceClipReader: ReferenceClipReader,
    private val evidenceSource: AttemptEvidenceSource,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

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
            audioPlayer.state.collect { state -> onPlaybackState(state) }
        }
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                _uiState.update { it.copy(showHangul = preferences.showHangul) }
            }
        }
    }

    fun load(moduleId: String, kindValue: String, specId: String) {
        val kind = SessionKind.fromRoute(kindValue)
        if (kind == null) {
            _uiState.update { it.copy(loading = false, notFound = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, notFound = false, errorMessage = null) }
            try {
                val module = registry.module(moduleId) ?: error("Module not found: $moduleId")
                val resolved = when (kind) {
                    SessionKind.LESSON -> {
                        val spec = module.lessons().firstOrNull { it.id == specId }
                            ?: error("Lesson not found: $specId")
                        ResolvedSpec(spec.title, DrillMode.LESSON, spec.contentItemIds)
                    }

                    SessionKind.PRACTICE -> {
                        val spec = module.practices().firstOrNull { it.id == specId }
                            ?: error("Practice not found: $specId")
                        ResolvedSpec(spec.title, spec.mode, spec.contentItemIds)
                    }
                }
                val items = contentRepository.contentItems(resolved.contentItemIds)
                _uiState.update {
                    it.copy(
                        loading = false,
                        notFound = false,
                        moduleTitle = module.title,
                        specTitle = resolved.title,
                        mode = resolved.mode,
                        items = items,
                        index = 0,
                        completedCount = 0,
                        finished = false,
                    )
                }
                prepareCurrentItem()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        notFound = true,
                        errorMessage = error.message ?: error::class.java.simpleName,
                    )
                }
            }
        }
    }

    fun onPermissionStatusChecked(granted: Boolean) {
        _uiState.update { state ->
            when {
                granted -> state.copy(permission = MicrophonePermission.Granted)
                state.permission == MicrophonePermission.Granted ->
                    state.copy(permission = MicrophonePermission.NotRequested)

                else -> state
            }
        }
    }

    fun onRequestPermission() {
        if (_uiState.value.permission == MicrophonePermission.Granted) return
        _uiState.update { it.copy(permission = MicrophonePermission.Requesting) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                permission = if (granted) {
                    MicrophonePermission.Granted
                } else {
                    MicrophonePermission.Denied
                },
            )
        }
    }

    fun onPlayReferenceClick() {
        val state = _uiState.value
        val item = state.currentItem ?: return
        if (state.processing || state.isRecording) return
        _uiState.update { it.copy(errorMessage = null) }
        audioPlayer.play(referenceSource(item))
    }

    fun onPlayAttemptClick() {
        val state = _uiState.value
        val audioRef = state.attemptAudioRef ?: return
        if (state.processing || state.isRecording) return
        _uiState.update { it.copy(errorMessage = null) }
        audioPlayer.play(AudioSource.LocalFile(audioRef))
    }

    /**
     * Requests compared reference-vs-attempt feedback for the recorded attempt (WF-1). The
     * bundled reference clip is read first; when it is missing the workflow is skipped and
     * the offline fallback is shown. Workflow failures also fall back rather than erroring.
     */
    fun onGetFeedbackClick() {
        val state = _uiState.value
        if (!state.isFeedbackMode || state.feedback is FeedbackUiState.Loading) return
        if (state.processing || state.isRecording) return
        val item = state.currentItem ?: return
        val attempt = attemptPcm ?: return
        _uiState.update { it.copy(feedback = FeedbackUiState.Loading) }
        viewModelScope.launch {
            val result = try {
                val referenceClip = referenceClipReader.read(contentRepository.audioAssetPath(item.audioAssetRef))
                if (referenceClip == null) {
                    null
                } else {
                    feedbackWorkflow.evaluate(
                        PronunciationFeedbackRequest(
                            pinyin = item.pinyin,
                            targetTones = item.targetTones,
                            referenceAudio = referenceClip,
                            attemptAudio = AudioClip(wavCodec.encode(attempt), AudioFormat.WAV),
                            acousticEvidence = evidenceSource.evidenceFor(item, attempt),
                        ),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                null
            }
            _uiState.update {
                it.copy(
                    feedback = when (result) {
                        is WorkflowResult.Success -> FeedbackUiState.Available(result.value)
                        else -> FeedbackUiState.OfflineFallback
                    },
                )
            }
        }
    }

    /**
     * Submits the recorded attempt to WF-4 to interpret the spoken answer (ADR 0009). The
     * transcript is matched against the same pinyin choices the tap drill shows; a match
     * selects and reveals that option, and any failure (no attempt, offline, worker not
     * configured, unmatched transcript) leaves the choices tappable instead. The workflow
     * never blocks advancing, and a late result never overwrites a tapped answer.
     */
    fun onAnswerClick() {
        val state = _uiState.value
        if (!state.isSpokenAnswerMode || state.answerRevealed || state.transcribing) return
        if (state.processing || state.isRecording) return
        val item = state.currentItem ?: return
        val attempt = attemptPcm ?: run {
            _uiState.update { it.copy(spokenAnswerFallback = true) }
            return
        }
        val options = state.choices
        _uiState.update { it.copy(transcribing = true, spokenAnswerFallback = false) }
        viewModelScope.launch {
            val result = try {
                responseWorkflow.transcribe(
                    ResponseTranscriptionRequest(
                        audio = AudioClip(wavCodec.encode(attempt), AudioFormat.WAV),
                        expectedOptions = options,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                null
            }
            applySpokenAnswer(result, options)
        }
    }

    private fun applySpokenAnswer(
        result: WorkflowResult<ResponseTranscription>?,
        options: List<String>,
    ) {
        val matched = when (result) {
            is WorkflowResult.Success -> result.value.matchedChoice(options)
            else -> null
        }
        val state = _uiState.value
        if (state.answerRevealed || !state.isSpokenAnswerMode) {
            _uiState.update { it.copy(transcribing = false) }
            return
        }
        if (matched == null) {
            _uiState.update { it.copy(transcribing = false, spokenAnswerFallback = true) }
            return
        }
        _uiState.update {
            it.copy(
                transcribing = false,
                spokenAnswerFallback = false,
                selectedChoice = matched,
                answerRevealed = true,
                answerCorrect = matched == it.correctAnswer,
            )
        }
    }

    /** Clears the attempt and coaching so the learner can record the item again. */
    fun onTryAgainClick() {
        val state = _uiState.value
        if (state.isRecording || state.processing) return
        attemptPcm = null
        _uiState.update {
            it.copy(
                attemptAudioRef = null,
                attemptPlayback = SessionPlayback(),
                feedback = FeedbackUiState.None,
                transcribing = false,
                spokenAnswerFallback = false,
                recorderError = null,
            )
        }
    }

    fun onChoiceSelected(choice: String) {
        val state = _uiState.value
        if (!state.isChoiceMode || state.answerRevealed) return
        _uiState.update {
            it.copy(
                selectedChoice = choice,
                answerRevealed = true,
                answerCorrect = choice == state.correctAnswer,
            )
        }
    }

    fun onRecordClick() {
        val state = _uiState.value
        when {
            state.processing -> Unit
            state.isRecording -> stopRecording()
            state.permission != MicrophonePermission.Granted -> onRequestPermission()
            else -> startRecording()
        }
    }

    fun onPrevious() {
        val state = _uiState.value
        if (state.index <= 0 || state.isRecording || state.processing) return
        attemptPcm = null
        _uiState.update {
            it.copy(
                index = it.index - 1,
                completedCount = it.index - 1,
                selectedChoice = null,
                answerRevealed = false,
                answerCorrect = null,
                correctAnswer = null,
                choices = emptyList(),
                referencePlayback = SessionPlayback(),
                attemptAudioRef = null,
                attemptPlayback = SessionPlayback(),
                feedback = FeedbackUiState.None,
                transcribing = false,
                spokenAnswerFallback = false,
                recorderState = RecorderState.Idle,
                level = 0f,
                errorMessage = null,
                recorderError = null,
            )
        }
        prepareCurrentItem()
    }

    fun onNext() {
        if (!_uiState.value.canAdvance) return
        viewModelScope.launch { advance() }
    }

    fun onLeaveScreen() {
        if (!_uiState.value.isRecording) return
        viewModelScope.launch {
            runCatching { audioRecorder.stop() }
        }
    }

    private suspend fun advance() {
        val state = _uiState.value
        val item = state.currentItem ?: return
        persistCurrent(item)
        val nextIndex = state.index + 1
        if (nextIndex >= state.items.size) {
            _uiState.update { it.copy(finished = true, completedCount = state.items.size) }
            return
        }
        attemptPcm = null
        _uiState.update {
            it.copy(
                index = nextIndex,
                completedCount = nextIndex,
                selectedChoice = null,
                answerRevealed = false,
                answerCorrect = null,
                correctAnswer = null,
                choices = emptyList(),
                referencePlayback = SessionPlayback(),
                recorderState = RecorderState.Idle,
                level = 0f,
                processing = false,
                transcribing = false,
                spokenAnswerFallback = false,
                attemptAudioRef = null,
                attemptPlayback = SessionPlayback(),
                feedback = FeedbackUiState.None,
                errorMessage = null,
                recorderError = null,
            )
        }
        prepareCurrentItem()
    }

    private fun prepareCurrentItem() {
        val state = _uiState.value
        val item = state.currentItem ?: return
        val mode = state.mode
        val choices = if (
            mode == DrillMode.HEAR_AND_NAME ||
            mode == DrillMode.LISTEN_AND_CHOOSE ||
            mode == DrillMode.LISTEN_AND_ANSWER_SPOKEN
        ) {
            DrillChoices.forItem(item, state.items, mode)
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                choices = choices,
                correctAnswer = mode?.let { resolvedMode -> DrillChoices.answerLabel(item, resolvedMode) },
            )
        }
        audioPlayer.play(referenceSource(item))
    }

    private suspend fun persistCurrent(item: ContentItem) {
        val now = clock.instant()
        val existing = progressRepository.byId(item.id)
        progressRepository.upsert(
            Progress(
                contentItemId = item.id,
                timesPracticed = (existing?.timesPracticed ?: 0) + 1,
                lastPracticedAt = now,
                feedbackThemes = existing?.feedbackThemes.orEmpty(),
            ),
        )
        val audioRef = _uiState.value.attemptAudioRef
        val speaks = _uiState.value.mode == DrillMode.SPEAK_AND_REPEAT ||
            _uiState.value.mode == DrillMode.SPEAK_AND_REPEAT_FEEDBACK
        if (speaks && audioRef != null) {
            attemptRepository.upsert(
                Attempt(
                    id = attemptId(item.id, now),
                    contentItemId = item.id,
                    recordedAt = now,
                    userAudioRef = audioRef,
                    feedbackText = (_uiState.value.feedback as? FeedbackUiState.Available)?.let { available ->
                        "${available.feedback.weakestUnit}: ${available.feedback.tip}"
                    },
                ),
            )
        }
    }

    private fun startRecording() {
        audioPlayer.pause()
        _uiState.update { it.copy(recorderError = null) }
        audioRecorder.start()
    }

    private fun stopRecording() {
        if (stopJob?.isActive == true) return
        _uiState.update { it.copy(processing = true) }
        stopJob = viewModelScope.launch {
            try {
                val audio = audioRecorder.stop()
                val file = recordingStore.newRecordingFile(SESSION_ATTEMPT_LABEL)
                wavCodec.write(file, audio)
                attemptPcm = audio
                _uiState.update {
                    it.copy(
                        attemptAudioRef = file.path,
                        recorderState = RecorderState.Idle,
                        feedback = FeedbackUiState.None,
                    )
                }
                audioPlayer.play(AudioSource.LocalFile(file.path))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(recorderError = error.message ?: error::class.java.simpleName) }
            } finally {
                _uiState.update { it.copy(processing = false) }
            }
            val state = _uiState.value
            if (state.isSpokenAnswerMode && state.attemptAudioRef != null) onAnswerClick()
        }
    }

    private fun onRecorderState(state: RecorderState) {
        _uiState.update {
            it.copy(
                recorderState = state,
                recorderError = (state as? RecorderState.Failed)?.message ?: it.recorderError,
            )
        }
    }

    private fun onPlaybackState(state: PlaybackState) {
        val item = _uiState.value.currentItem
        val reference = item?.let(::referenceSource)
        when {
            reference != null && state.source == reference ->
                _uiState.update { it.copy(referencePlayback = it.referencePlayback.with(state)) }

            isAttemptSource(state.source) ->
                _uiState.update { it.copy(attemptPlayback = it.attemptPlayback.with(state)) }
        }
        (state.status as? PlaybackStatus.Failed)?.let { failure ->
            _uiState.update { it.copy(errorMessage = failure.message) }
        }
    }

    private fun referenceSource(item: ContentItem): AudioSource = AudioSource.Asset(contentRepository.audioAssetPath(item.audioAssetRef))

    private fun isAttemptSource(source: AudioSource?): Boolean {
        val audioRef = _uiState.value.attemptAudioRef ?: return false
        return source is AudioSource.LocalFile && source.path == audioRef
    }

    /**
     * Resolves a WF-4 result to one of the drill's pinyin choices: its `matchedOptionId`
     * when it names a choice, otherwise its transcript compared case-insensitively, then
     * tone-insensitively when that leaves exactly one candidate. An ambiguous transcript
     * (for example `ma` against `ma1` and `ma2`) resolves to nothing so the learner taps.
     */
    private fun ResponseTranscription.matchedChoice(options: List<String>): String? {
        matchedOptionId?.let { id ->
            options.firstOrNull { it == id }?.let { return it }
            options.singleOrNull { normalizedAnswer(it) == normalizedAnswer(id) }?.let { return it }
        }
        options.firstOrNull { it.equals(transcript, ignoreCase = true) }?.let { return it }
        val normalizedTranscript = normalizedAnswer(transcript)
        if (normalizedTranscript.isEmpty()) return null
        return options.singleOrNull { normalizedAnswer(it) == normalizedTranscript }
    }

    private fun normalizedAnswer(value: String): String = value
        .lowercase(Locale.ROOT)
        .filter { it.isLetter() }

    private fun SessionPlayback.with(playback: PlaybackState): SessionPlayback = SessionPlayback(
        state = playback,
        hasPlayed = hasPlayed || playback.source != null,
    )

    private fun attemptId(contentItemId: String, recordedAt: Instant): String = "attempt-$contentItemId-${recordedAt.toEpochMilli()}"

    private data class ResolvedSpec(
        val title: String,
        val mode: DrillMode,
        val contentItemIds: List<String>,
    )
}
