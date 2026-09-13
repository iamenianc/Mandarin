package com.learnhuayu.app.ui.session

import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.app.ui.audio.FakeAudioPlayer
import com.learnhuayu.app.ui.audio.FakeAudioRecorder
import com.learnhuayu.app.ui.audio.FakeRecordingStore
import com.learnhuayu.app.ui.audio.FakeWavCodec
import com.learnhuayu.app.ui.audio.MainDispatcherRule
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.playback.AudioSource
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.PracticeSpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Voice-activity auto-stop for the speaking drills. The recorder signals end of speech and
 * the view model applies it through the same stop path the manual button uses. The signal
 * must fire the attempt handling at most once, be ignored when no recording is active or a
 * stop is in flight, and never affect a mode without a spoken attempt.
 */
class SessionVoiceAutoStopTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val player = FakeAudioPlayer()
    private val recorder = FakeAudioRecorder()
    private val wavCodec = FakeWavCodec()
    private val recordingStore = FakeRecordingStore(attemptFile)
    private val progressRepository = FakeProgressRepository()
    private val attemptRepository = FakeAttemptRepository()
    private val responseWorkflow = FakeResponseTranscriptionWorkflow()
    private val contentRepository = FakeBundledContentRepository(items = itemsById)
    private val registry = ModuleRegistry(
        setOf(
            FakeLearningModule(
                id = MODULE_ID,
                title = "Speech",
                practiceSpecs = listOf(speakSpec, feedbackSpec, spokenSpec, hearAndNameSpec),
            ),
        ),
    )

    private fun createViewModel(): SessionViewModel = SessionViewModel(
        registry = registry,
        contentRepository = contentRepository,
        audioPlayer = player,
        audioRecorder = recorder,
        wavCodec = wavCodec,
        recordingStore = recordingStore,
        progressRepository = progressRepository,
        attemptRepository = attemptRepository,
        preferencesRepository = FakePreferencesRepository(),
        feedbackWorkflow = FakePronunciationFeedbackWorkflow(WorkflowResult.Failure(WorkflowFailure.BaseUrlMissing())),
        responseWorkflow = responseWorkflow,
        referenceClipReader = FakeReferenceClipReader(),
        evidenceSource = FakeAttemptEvidenceSource(),
        clock = fixedClock,
    )

    private fun SessionViewModel.startRecording(spec: PracticeSpec) {
        onPermissionStatusChecked(true)
        load(MODULE_ID, "practice", spec.id)
        onRecordClick()
    }

    @Test
    fun `end of speech stops the recording once and handles the attempt`() = runTest {
        val viewModel = createViewModel()
        viewModel.startRecording(speakSpec)
        assertTrue(viewModel.uiState.value.isRecording)

        recorder.signalSpeechEnded()

        assertEquals(1, recorder.stopCount)
        assertEquals(attemptFile, wavCodec.writtenFile)
        assertEquals(attemptFile.path, viewModel.uiState.value.attemptAudioRef)
        assertFalse(viewModel.uiState.value.isRecording)
        assertFalse(viewModel.uiState.value.processing)
        assertTrue(player.playedSources.contains(AudioSource.LocalFile(attemptFile.path)))
    }

    @Test
    fun `a duplicate end of speech signal while the stop is in flight is ignored`() = runTest {
        val viewModel = createViewModel()
        viewModel.startRecording(speakSpec)
        recorder.stopGate = CompletableDeferred()

        recorder.signalSpeechEnded()
        recorder.signalSpeechEnded()

        assertEquals(1, recorder.stopCount)
        assertTrue(viewModel.uiState.value.processing)

        recorder.stopGate?.complete(Unit)

        assertEquals(1, recorder.stopCount)
        assertFalse(viewModel.uiState.value.processing)
    }

    @Test
    fun `a late end of speech signal after the attempt is handled is ignored`() = runTest {
        val viewModel = createViewModel()
        viewModel.startRecording(speakSpec)
        recorder.signalSpeechEnded()
        assertEquals(1, recorder.stopCount)

        recorder.signalSpeechEnded()

        assertEquals(1, recorder.stopCount)
        assertEquals(attemptFile.path, viewModel.uiState.value.attemptAudioRef)
    }

    @Test
    fun `end of speech before any recording is ignored`() = runTest {
        val viewModel = createViewModel()
        viewModel.onPermissionStatusChecked(true)
        viewModel.load(MODULE_ID, "practice", speakSpec.id)

        recorder.signalSpeechEnded()

        assertEquals(0, recorder.stopCount)
    }

    @Test
    fun `the manual stop button still stops and a later signal is ignored`() = runTest {
        val viewModel = createViewModel()
        viewModel.startRecording(speakSpec)

        viewModel.onRecordClick()

        assertEquals(1, recorder.stopCount)
        assertEquals(attemptFile.path, viewModel.uiState.value.attemptAudioRef)

        recorder.signalSpeechEnded()

        assertEquals(1, recorder.stopCount)
    }

    @Test
    fun `feedback mode auto-stops the recording`() = runTest {
        val viewModel = createViewModel()
        viewModel.startRecording(feedbackSpec)

        recorder.signalSpeechEnded()

        assertEquals(1, recorder.stopCount)
        assertEquals(DrillMode.SPEAK_AND_REPEAT_FEEDBACK, viewModel.uiState.value.mode)
        assertEquals(attemptFile.path, viewModel.uiState.value.attemptAudioRef)
    }

    @Test
    fun `spoken answer mode auto-stops and submits the attempt`() = runTest {
        responseWorkflow.returns(WorkflowResult.Failure(WorkflowFailure.BaseUrlMissing()))
        val viewModel = createViewModel()
        viewModel.startRecording(spokenSpec)

        recorder.signalSpeechEnded()

        assertEquals(1, recorder.stopCount)
        assertEquals(DrillMode.LISTEN_AND_ANSWER_SPOKEN, viewModel.uiState.value.mode)
        assertEquals(1, responseWorkflow.requests.size)
        assertTrue(viewModel.uiState.value.spokenAnswerFallback)
    }

    @Test
    fun `a mode without a spoken attempt does not auto-stop`() = runTest {
        val viewModel = createViewModel()
        viewModel.startRecording(hearAndNameSpec)

        recorder.signalSpeechEnded()

        assertEquals(0, recorder.stopCount)
        assertTrue(viewModel.uiState.value.isRecording)
    }

    @Test
    fun `a speech-started signal does not stop the recording`() = runTest {
        val viewModel = createViewModel()
        viewModel.startRecording(speakSpec)

        recorder.signalSpeechStarted()

        assertEquals(0, recorder.stopCount)
        assertTrue(viewModel.uiState.value.isRecording)
    }

    private companion object {
        const val MODULE_ID = "speech"
        val recordedAt: Instant = Instant.parse("2026-09-13T09:00:00Z")
        val fixedClock: Clock = Clock.fixed(recordedAt, ZoneOffset.UTC)
        val attemptFile = File("build/tmp/session-voice-auto-stop-test/attempt.wav")

        val ma1 = item("speech-ma1", "ma1", listOf(1))
        val ma2 = item("speech-ma2", "ma2", listOf(2))
        val ma3 = item("speech-ma3", "ma3", listOf(3))
        val ma4 = item("speech-ma4", "ma4", listOf(4))

        val itemsById: Map<String, ContentItem> = listOf(ma1, ma2, ma3, ma4).associateBy { it.id }

        val speakSpec = PracticeSpec(
            id = "speech-practice-say-and-repeat",
            moduleId = MODULE_ID,
            title = "Say and repeat",
            contentItemIds = listOf(ma1.id, ma2.id),
            mode = DrillMode.SPEAK_AND_REPEAT,
        )

        val feedbackSpec = PracticeSpec(
            id = "speech-practice-say-and-repeat-feedback",
            moduleId = MODULE_ID,
            title = "Say and repeat with feedback",
            contentItemIds = listOf(ma1.id, ma2.id),
            mode = DrillMode.SPEAK_AND_REPEAT_FEEDBACK,
        )

        val spokenSpec = PracticeSpec(
            id = "speech-practice-spoken-answer",
            moduleId = MODULE_ID,
            title = "Say what is heard",
            contentItemIds = listOf(ma1.id, ma2.id, ma3.id, ma4.id),
            mode = DrillMode.LISTEN_AND_ANSWER_SPOKEN,
        )

        val hearAndNameSpec = PracticeSpec(
            id = "speech-practice-hear-and-name",
            moduleId = MODULE_ID,
            title = "Name the tone",
            contentItemIds = listOf(ma1.id, ma2.id, ma3.id, ma4.id),
            mode = DrillMode.HEAR_AND_NAME,
        )

        fun item(id: String, pinyin: String, targetTones: List<Int>): ContentItem = ContentItem(
            id = id,
            type = ContentItemType.WORD,
            source = ContentSource.BUNDLED,
            meaning = pinyin,
            audioAssetRef = "reference/speech/$id.ogg",
            pinyin = pinyin,
            targetTones = targetTones,
        )
    }
}
