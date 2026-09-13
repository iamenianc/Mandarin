package com.learnhuayu.app.ui.session

import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.app.ui.audio.FakeAudioPlayer
import com.learnhuayu.app.ui.audio.FakeAudioRecorder
import com.learnhuayu.app.ui.audio.FakeRecordingStore
import com.learnhuayu.app.ui.audio.FakeWavCodec
import com.learnhuayu.app.ui.audio.MainDispatcherRule
import com.learnhuayu.core.ai.AcousticEvidence
import com.learnhuayu.core.ai.AudioClip
import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.PronunciationFeedback
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.PracticeSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SessionFeedbackTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val player = FakeAudioPlayer()
    private val recorder = FakeAudioRecorder()
    private val wavCodec = FakeWavCodec()
    private val recordingStore = FakeRecordingStore(attemptFile)
    private val progressRepository = FakeProgressRepository()
    private val attemptRepository = FakeAttemptRepository()
    private val workflow = FakePronunciationFeedbackWorkflow(WorkflowResult.Success(feedback))
    private val responseWorkflow = FakeResponseTranscriptionWorkflow()
    private val evidenceSource = FakeAttemptEvidenceSource(evidence)
    private val contentRepository = FakeBundledContentRepository(items = mapOf(ma1.id to ma1))
    private val registry = ModuleRegistry(
        setOf(
            FakeLearningModule(
                id = "speech",
                title = "Speech",
                practiceSpecs = listOf(speakAndRepeatSpec),
            ),
        ),
    )

    private fun createViewModel(
        referenceClipReader: ReferenceClipReader = FakeReferenceClipReader(
            mapOf(REFERENCE_ASSET_PATH to AudioClip(referenceBytes, AudioFormat.OGG)),
        ),
    ): SessionViewModel = SessionViewModel(
        registry = registry,
        contentRepository = contentRepository,
        audioPlayer = player,
        audioRecorder = recorder,
        wavCodec = wavCodec,
        recordingStore = recordingStore,
        progressRepository = progressRepository,
        attemptRepository = attemptRepository,
        preferencesRepository = FakePreferencesRepository(),
        feedbackWorkflow = workflow,
        responseWorkflow = responseWorkflow,
        exerciseWorkflow = FakeExerciseGenerationWorkflow(),
        referenceClipReader = referenceClipReader,
        evidenceSource = evidenceSource,
        clock = fixedClock,
    )

    private fun SessionViewModel.recordAttempt() {
        onPermissionStatusChecked(true)
        load("speech", "practice", speakAndRepeatSpec.id)
        onRecordClick()
        onRecordClick()
    }

    @Test
    fun `feedback request carries pinyin, target tones, and both clips`() = runTest {
        val reader = FakeReferenceClipReader(
            mapOf(REFERENCE_ASSET_PATH to AudioClip(referenceBytes, AudioFormat.OGG)),
        )
        val viewModel = createViewModel(referenceClipReader = reader)
        viewModel.recordAttempt()

        viewModel.onGetFeedbackClick()

        val request = workflow.requests.single()
        assertEquals("ma1", request.pinyin)
        assertEquals(listOf(1), request.targetTones)
        assertEquals(AudioFormat.OGG, request.referenceAudio.format)
        assertArrayEquals(referenceBytes, request.referenceAudio.bytes)
        assertEquals(AudioFormat.WAV, request.attemptAudio.format)
        assertEquals(ATTEMPT_BYTES, request.attemptAudio.bytes.size)
        assertEquals(evidence, request.acousticEvidence)
        assertEquals(listOf(ma1 to recorder.stopResult), evidenceSource.calls)
        assertEquals(listOf(REFERENCE_ASSET_PATH), reader.requestedPaths)
        assertEquals(attemptFile.path, viewModel.uiState.value.attemptAudioRef)
    }

    @Test
    fun `success renders the coaching fields`() = runTest {
        val viewModel = createViewModel()
        viewModel.recordAttempt()

        viewModel.onGetFeedbackClick()

        assertEquals(FeedbackUiState.Available(feedback), viewModel.uiState.value.feedback)
        val available = viewModel.uiState.value.feedback as FeedbackUiState.Available
        assertEquals("ma1", available.feedback.weakestUnit)
        assertEquals("the tone is flat", available.feedback.issue)
        assertEquals("start higher", available.feedback.tip)
        assertEquals("good effort", available.feedback.encouragement)
        assertEquals("listen to ma1 again", available.feedback.replayHint)
    }

    @Test
    fun `a workflow failure renders the offline fallback`() = runTest {
        workflow.returns(WorkflowResult.Failure(WorkflowFailure.NetworkError("offline")))
        val viewModel = createViewModel()
        viewModel.recordAttempt()

        viewModel.onGetFeedbackClick()

        assertEquals(FeedbackUiState.OfflineFallback, viewModel.uiState.value.feedback)
        assertTrue(viewModel.uiState.value.canAdvance)
    }

    @Test
    fun `a missing reference clip skips the workflow and shows the fallback`() = runTest {
        val viewModel = createViewModel(referenceClipReader = FakeReferenceClipReader())
        viewModel.recordAttempt()

        viewModel.onGetFeedbackClick()

        assertTrue(workflow.requests.isEmpty())
        assertTrue(evidenceSource.calls.isEmpty())
        assertEquals(FeedbackUiState.OfflineFallback, viewModel.uiState.value.feedback)
    }

    @Test
    fun `try again clears the attempt and the coaching`() = runTest {
        val viewModel = createViewModel()
        viewModel.recordAttempt()
        viewModel.onGetFeedbackClick()

        viewModel.onTryAgainClick()

        assertEquals(FeedbackUiState.None, viewModel.uiState.value.feedback)
        assertEquals(null, viewModel.uiState.value.attemptAudioRef)
    }

    @Test
    fun `an attempt can advance even when no voice was detected`() = runTest {
        recorder.stopResult = PcmAudio(ShortArray(0), 24_000)
        val viewModel = createViewModel()
        viewModel.recordAttempt()

        assertTrue(viewModel.uiState.value.canAdvance)

        viewModel.onNext()

        assertTrue(viewModel.uiState.value.finished)
    }

    private companion object {
        val recordedAt: Instant = Instant.parse("2026-09-13T09:00:00Z")
        val fixedClock: Clock = Clock.fixed(recordedAt, ZoneOffset.UTC)
        val attemptFile = File("build/tmp/session-feedback-test/attempt.wav")
        const val REFERENCE_ASSET_PATH = "audio/reference/speech/speech-ma1.ogg"
        const val ATTEMPT_SAMPLES = 240
        const val ATTEMPT_BYTES = ATTEMPT_SAMPLES * 2
        val referenceBytes = byteArrayOf(1, 2, 3, 4)
        val ma1 = ContentItem(
            id = "speech-ma1",
            type = ContentItemType.WORD,
            source = ContentSource.BUNDLED,
            meaning = "mother (first tone)",
            audioAssetRef = "reference/speech/speech-ma1.ogg",
            pinyin = "ma1",
            targetTones = listOf(1),
        )
        val evidence = AcousticEvidence(
            phrase = "ma1",
            syllables = emptyList(),
        )
        val feedback = PronunciationFeedback(
            weakestUnit = "ma1",
            issue = "the tone is flat",
            tip = "start higher",
            encouragement = "good effort",
            replayHint = "listen to ma1 again",
        )
        val speakAndRepeatSpec = PracticeSpec(
            id = "speech-practice-say-the-tone",
            moduleId = "speech",
            title = "Say the tone practice",
            contentItemIds = listOf(ma1.id),
            mode = DrillMode.SPEAK_AND_REPEAT_FEEDBACK,
        )
    }
}
