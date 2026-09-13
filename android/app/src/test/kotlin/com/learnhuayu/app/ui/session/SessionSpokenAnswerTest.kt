package com.learnhuayu.app.ui.session

import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.app.ui.audio.FakeAudioPlayer
import com.learnhuayu.app.ui.audio.FakeAudioRecorder
import com.learnhuayu.app.ui.audio.FakeRecordingStore
import com.learnhuayu.app.ui.audio.FakeWavCodec
import com.learnhuayu.app.ui.audio.MainDispatcherRule
import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.ResponseTranscription
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.PracticeSpec
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
 * The spoken-answer listening drill (WF-4, ADR 0009). Each test drives the shared record
 * flow; stopping the recording submits the attempt, and the fake workflow decides the
 * result. A match reveals the answer through the same choice path the tap drills use; any
 * failure leaves the choices tappable so the drill still completes offline.
 */
class SessionSpokenAnswerTest {

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
                title = "Listening",
                practiceSpecs = listOf(spokenSpec, hearAndNameSpec, listenAndChooseSpec),
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
        exerciseWorkflow = FakeExerciseGenerationWorkflow(),
        referenceClipReader = FakeReferenceClipReader(),
        evidenceSource = FakeAttemptEvidenceSource(),
        clock = fixedClock,
    )

    private fun SessionViewModel.recordSpokenAttempt(spec: PracticeSpec = spokenSpec) {
        onPermissionStatusChecked(true)
        load(MODULE_ID, "practice", spec.id)
        onRecordClick()
        onRecordClick()
    }

    @Test
    fun `a matched transcript selects and reveals the right option`() = runTest {
        responseWorkflow.returns(success(transcript = "NI3"))
        val viewModel = createViewModel()

        viewModel.recordSpokenAttempt()

        assertEquals(DrillMode.LISTEN_AND_ANSWER_SPOKEN, viewModel.uiState.value.mode)
        assertEquals(listOf("hao3", "ni3", "xie4", "zai4"), viewModel.uiState.value.choices)
        assertEquals("ni3", viewModel.uiState.value.selectedChoice)
        assertTrue(viewModel.uiState.value.answerRevealed)
        assertEquals(true, viewModel.uiState.value.answerCorrect)
        assertFalse(viewModel.uiState.value.spokenAnswerFallback)
        assertTrue(viewModel.uiState.value.canAdvance)
    }

    @Test
    fun `a tone-insensitive transcript still matches one option`() = runTest {
        responseWorkflow.returns(success(transcript = "NI"))
        val viewModel = createViewModel()

        viewModel.recordSpokenAttempt()

        assertEquals("ni3", viewModel.uiState.value.selectedChoice)
        assertEquals(true, viewModel.uiState.value.answerCorrect)
    }

    @Test
    fun `a matchedOptionId selects that option and reveals it`() = runTest {
        responseWorkflow.returns(success(transcript = "", matchedOptionId = "xie4"))
        val viewModel = createViewModel()

        viewModel.recordSpokenAttempt()

        assertEquals("xie4", viewModel.uiState.value.selectedChoice)
        assertTrue(viewModel.uiState.value.answerRevealed)
        assertEquals(false, viewModel.uiState.value.answerCorrect)
    }

    @Test
    fun `the request carries the attempt clip and the expected options`() = runTest {
        responseWorkflow.returns(success(transcript = "ni3"))
        val viewModel = createViewModel()

        viewModel.recordSpokenAttempt()

        val request = responseWorkflow.requests.single()
        assertEquals(AudioFormat.WAV, request.audio.format)
        assertEquals(ATTEMPT_BYTES, request.audio.bytes.size)
        assertEquals(listOf("hao3", "ni3", "xie4", "zai4"), request.expectedOptions)
        assertEquals(attemptFile.path, viewModel.uiState.value.attemptAudioRef)
    }

    @Test
    fun `an unmatched transcript falls back to tappable choices`() = runTest {
        responseWorkflow.returns(success(transcript = "guten tag"))
        val viewModel = createViewModel()

        viewModel.recordSpokenAttempt()

        assertFalse(viewModel.uiState.value.answerRevealed)
        assertTrue(viewModel.uiState.value.spokenAnswerFallback)
        assertEquals(listOf("hao3", "ni3", "xie4", "zai4"), viewModel.uiState.value.choices)

        viewModel.onChoiceSelected("ni3")

        assertTrue(viewModel.uiState.value.answerRevealed)
        assertEquals(true, viewModel.uiState.value.answerCorrect)
    }

    @Test
    fun `a workflow failure leaves the tap choices available offline`() = runTest {
        responseWorkflow.returns(WorkflowResult.Failure(WorkflowFailure.NetworkError("offline")))
        val viewModel = createViewModel()

        viewModel.recordSpokenAttempt()

        assertFalse(viewModel.uiState.value.answerRevealed)
        assertTrue(viewModel.uiState.value.spokenAnswerFallback)
        assertFalse(viewModel.uiState.value.canAdvance)

        viewModel.onChoiceSelected("ni3")
        viewModel.onNext()

        assertEquals(1, viewModel.uiState.value.index)
        assertEquals(listOf(ni3.id), progressRepository.upserts.map { it.contentItemId })
    }

    @Test
    fun `a missing attempt offers the tap choices without calling the workflow`() = runTest {
        val viewModel = createViewModel()
        viewModel.onPermissionStatusChecked(true)
        viewModel.load(MODULE_ID, "practice", spokenSpec.id)

        viewModel.onAnswerClick()

        assertTrue(responseWorkflow.requests.isEmpty())
        assertTrue(viewModel.uiState.value.spokenAnswerFallback)
        assertFalse(viewModel.uiState.value.answerRevealed)
    }

    @Test
    fun `the answer action resubmits after a failed transcription`() = runTest {
        responseWorkflow.returns(WorkflowResult.Failure(WorkflowFailure.NetworkError("offline")))
        val viewModel = createViewModel()
        viewModel.recordSpokenAttempt()
        assertEquals(1, responseWorkflow.requests.size)

        responseWorkflow.returns(success(transcript = "ni3"))
        viewModel.onAnswerClick()

        assertEquals(2, responseWorkflow.requests.size)
        assertEquals("ni3", viewModel.uiState.value.selectedChoice)
        assertTrue(viewModel.uiState.value.answerRevealed)
        assertFalse(viewModel.uiState.value.spokenAnswerFallback)
    }

    @Test
    fun `tone and tap-only drills never call the response workflow`() = runTest {
        val viewModel = createViewModel()

        viewModel.load(MODULE_ID, "practice", hearAndNameSpec.id)
        viewModel.onChoiceSelected("3")
        assertTrue(viewModel.uiState.value.answerRevealed)

        viewModel.load(MODULE_ID, "practice", listenAndChooseSpec.id)
        viewModel.onChoiceSelected("ni3")
        assertTrue(viewModel.uiState.value.answerRevealed)

        assertTrue(responseWorkflow.requests.isEmpty())
    }

    private fun success(
        transcript: String,
        matchedOptionId: String? = null,
    ): WorkflowResult<ResponseTranscription> = WorkflowResult.Success(
        ResponseTranscription(transcript = transcript, matchedOptionId = matchedOptionId),
    )

    private companion object {
        const val MODULE_ID = "listening"
        val recordedAt: Instant = Instant.parse("2026-09-13T09:00:00Z")
        val fixedClock: Clock = Clock.fixed(recordedAt, ZoneOffset.UTC)
        val attemptFile = File("build/tmp/session-spoken-answer-test/attempt.wav")
        const val ATTEMPT_SAMPLES = 240
        const val ATTEMPT_BYTES = ATTEMPT_SAMPLES * 2

        val ni3 = item("listening-ni3", "ni3", listOf(3))
        val hao3 = item("listening-hao3", "hao3", listOf(3))
        val xie4 = item("listening-xie4", "xie4", listOf(4))
        val zai4 = item("listening-zai4", "zai4", listOf(4))

        val itemsById: Map<String, ContentItem> = listOf(ni3, hao3, xie4, zai4).associateBy { it.id }

        val spokenSpec = PracticeSpec(
            id = "listening-practice-hear-the-word",
            moduleId = MODULE_ID,
            title = "Hear the word",
            contentItemIds = listOf(ni3.id, hao3.id, xie4.id, zai4.id),
            mode = DrillMode.LISTEN_AND_ANSWER_SPOKEN,
        )

        val hearAndNameSpec = PracticeSpec(
            id = "listening-practice-hear-the-tone",
            moduleId = MODULE_ID,
            title = "Hear the tone",
            contentItemIds = listOf(ni3.id, hao3.id),
            mode = DrillMode.HEAR_AND_NAME,
        )

        val listenAndChooseSpec = PracticeSpec(
            id = "listening-practice-tap-the-word",
            moduleId = MODULE_ID,
            title = "Tap the word",
            contentItemIds = listOf(ni3.id, hao3.id),
            mode = DrillMode.LISTEN_AND_CHOOSE,
        )

        fun item(id: String, pinyin: String, targetTones: List<Int>): ContentItem = ContentItem(
            id = id,
            type = ContentItemType.WORD,
            source = ContentSource.BUNDLED,
            meaning = pinyin,
            audioAssetRef = "reference/listening/$id.ogg",
            pinyin = pinyin,
            targetTones = targetTones,
        )
    }
}
