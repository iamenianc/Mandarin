package com.learnhuayu.app.ui.session

import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.app.ui.audio.FakeAudioPlayer
import com.learnhuayu.app.ui.audio.FakeAudioRecorder
import com.learnhuayu.app.ui.audio.FakeRecordingStore
import com.learnhuayu.app.ui.audio.FakeWavCodec
import com.learnhuayu.app.ui.audio.MainDispatcherRule
import com.learnhuayu.core.ai.ExerciseItemType
import com.learnhuayu.core.ai.GeneratedExercise
import com.learnhuayu.core.ai.GeneratedExercises
import com.learnhuayu.core.ai.PronunciationFeedback
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.LessonSpec
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
 * Endless practice: extending a practice with runtime-generated exercises (WF-8, ADR 0010).
 * Each test loads a practice, answers through to the last bundled item, and then requests
 * extra items. The fake workflow decides the result: valid items are admitted as extra,
 * malformed or duplicate items are dropped, and an empty result or a failure keeps the
 * session finishable with a friendly message.
 */
class SessionExtraPracticeTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val player = FakeAudioPlayer()
    private val recorder = FakeAudioRecorder()
    private val wavCodec = FakeWavCodec()
    private val recordingStore = FakeRecordingStore(attemptFile)
    private val progressRepository = FakeProgressRepository()
    private val attemptRepository = FakeAttemptRepository()
    private val responseWorkflow = FakeResponseTranscriptionWorkflow()
    private val exerciseWorkflow = FakeExerciseGenerationWorkflow()
    private val contentRepository = FakeBundledContentRepository(items = itemsById)
    private val registry = ModuleRegistry(
        setOf(
            FakeLearningModule(
                id = MODULE_ID,
                title = "Tones",
                lessonSpecs = listOf(lessonSpec),
                practiceSpecs = listOf(listenAndChooseSpec),
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
        feedbackWorkflow = FakePronunciationFeedbackWorkflow(
            WorkflowResult.Success(
                PronunciationFeedback(
                    weakestUnit = "ma1",
                    issue = "the tone is flat",
                    tip = "start higher",
                    encouragement = "good effort",
                    replayHint = "listen to ma1 again",
                ),
            ),
        ),
        responseWorkflow = responseWorkflow,
        exerciseWorkflow = exerciseWorkflow,
        referenceClipReader = FakeReferenceClipReader(),
        evidenceSource = FakeAttemptEvidenceSource(),
        clock = fixedClock,
    )

    private fun SessionViewModel.reachLastBundledItem() {
        load(MODULE_ID, "practice", listenAndChooseSpec.id)
        onChoiceSelected("ma1")
        onNext()
        onChoiceSelected("ma2")
    }

    @Test
    fun `the extra action appears only on the last bundled item of a practice`() = runTest {
        val viewModel = createViewModel()

        viewModel.load(MODULE_ID, "practice", listenAndChooseSpec.id)
        viewModel.onChoiceSelected("ma1")

        assertFalse(viewModel.uiState.value.showKeepPractising)

        viewModel.onNext()
        viewModel.onChoiceSelected("ma2")

        assertTrue(viewModel.uiState.value.isLastItem)
        assertTrue(viewModel.uiState.value.showKeepPractising)

        viewModel.load(MODULE_ID, "lesson", lessonSpec.id)

        assertFalse(viewModel.uiState.value.showKeepPractising)
    }

    @Test
    fun `a valid result is admitted as extra items with stable ids`() = runTest {
        exerciseWorkflow.returns(
            WorkflowResult.Success(
                GeneratedExercises(
                    items = listOf(
                        GeneratedExercise(
                            type = ExerciseItemType.WORD,
                            meaning = "horse",
                            pinyin = "ma3",
                            targetTones = listOf(3),
                            rationale = "same syllable family",
                        ),
                        GeneratedExercise(
                            type = ExerciseItemType.WORD,
                            meaning = "scold",
                            pinyin = "ma4",
                            targetTones = listOf(4),
                            rationale = "same syllable family",
                        ),
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        viewModel.reachLastBundledItem()

        viewModel.onKeepPractisingClick()

        val state = viewModel.uiState.value
        assertEquals(4, state.items.size)
        val extras = state.items.drop(2)
        assertTrue(extras.all { it.source == ContentSource.GENERATED })
        assertEquals(
            listOf("extra-${listenAndChooseSpec.id}-1", "extra-${listenAndChooseSpec.id}-2"),
            extras.map { it.id },
        )
        assertEquals(listOf("ma3", "ma4"), extras.map { it.pinyin })
        assertTrue(state.extraAvailable)
        assertEquals(null, state.extraMessage)
        assertFalse(state.finished)
        assertFalse(state.showKeepPractising)
    }

    @Test
    fun `the request carries the module, item type, theme, units, and count`() = runTest {
        exerciseWorkflow.returns(WorkflowResult.Success(GeneratedExercises()))
        val viewModel = createViewModel()
        viewModel.reachLastBundledItem()

        viewModel.onKeepPractisingClick()

        val request = exerciseWorkflow.requests.single()
        assertEquals(MODULE_ID, request.moduleId)
        assertEquals(ExerciseItemType.WORD, request.itemType)
        assertEquals("Tones", request.theme)
        assertEquals(listOf("ma1", "ma2", "tone1", "tone2"), request.targetUnits)
        assertEquals(EXTRA_GENERATION_COUNT, request.count)
    }

    @Test
    fun `invalid and duplicate items are dropped while valid ones continue the session`() = runTest {
        exerciseWorkflow.returns(
            WorkflowResult.Success(
                GeneratedExercises(
                    items = listOf(
                        GeneratedExercise(
                            type = ExerciseItemType.WORD,
                            meaning = "hanzi item",
                            pinyin = "ma\u6C49",
                            targetTones = listOf(4),
                            rationale = "invalid",
                        ),
                        GeneratedExercise(
                            type = ExerciseItemType.WORD,
                            meaning = "toneless item",
                            pinyin = "ma",
                            targetTones = listOf(1),
                            rationale = "invalid",
                        ),
                        GeneratedExercise(
                            type = ExerciseItemType.WORD,
                            meaning = "bundled duplicate",
                            pinyin = "ma1",
                            targetTones = listOf(1),
                            rationale = "duplicate",
                        ),
                        GeneratedExercise(
                            type = ExerciseItemType.WORD,
                            meaning = "kept item",
                            pinyin = "ma3",
                            targetTones = listOf(3),
                            rationale = "valid",
                        ),
                        GeneratedExercise(
                            type = ExerciseItemType.WORD,
                            meaning = "same-batch duplicate",
                            pinyin = "ma3",
                            targetTones = listOf(3),
                            rationale = "duplicate",
                        ),
                        GeneratedExercise(
                            type = ExerciseItemType.WORD,
                            meaning = "blank tones",
                            pinyin = "ma4",
                            targetTones = emptyList(),
                            rationale = "invalid",
                        ),
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        viewModel.reachLastBundledItem()

        viewModel.onKeepPractisingClick()

        val state = viewModel.uiState.value
        assertEquals(listOf("ma1", "ma2", "ma3"), state.items.map { it.pinyin })
        val extra = state.items.last()
        assertEquals(ContentSource.GENERATED, extra.source)
        assertEquals("extra-${listenAndChooseSpec.id}-1", extra.id)
        assertTrue(state.extraAvailable)
        assertFalse(state.finished)
    }

    @Test
    fun `an empty result keeps the session finishable with a message`() = runTest {
        exerciseWorkflow.returns(WorkflowResult.Success(GeneratedExercises()))
        val viewModel = createViewModel()
        viewModel.reachLastBundledItem()

        viewModel.onKeepPractisingClick()

        val state = viewModel.uiState.value
        assertEquals(EXTRA_UNAVAILABLE_MESSAGE, state.extraMessage)
        assertFalse(state.extraAvailable)
        assertEquals(2, state.items.size)
        assertFalse(state.finished)

        viewModel.onChoiceSelected("ma2")
        viewModel.onNext()

        assertTrue(viewModel.uiState.value.finished)
    }

    @Test
    fun `a workflow failure keeps the session finishable with a message`() = runTest {
        exerciseWorkflow.returns(WorkflowResult.Failure(WorkflowFailure.NetworkError("offline")))
        val viewModel = createViewModel()
        viewModel.reachLastBundledItem()

        viewModel.onKeepPractisingClick()

        val state = viewModel.uiState.value
        assertEquals(EXTRA_UNAVAILABLE_MESSAGE, state.extraMessage)
        assertEquals(2, state.items.size)
        assertFalse(state.finished)

        viewModel.onChoiceSelected("ma2")
        viewModel.onNext()

        assertTrue(viewModel.uiState.value.finished)
    }

    @Test
    fun `a thrown failure keeps the session finishable with a message`() = runTest {
        exerciseWorkflow.throws(IllegalStateException("worker down"))
        val viewModel = createViewModel()
        viewModel.reachLastBundledItem()

        viewModel.onKeepPractisingClick()

        val state = viewModel.uiState.value
        assertEquals(EXTRA_UNAVAILABLE_MESSAGE, state.extraMessage)
        assertEquals(2, state.items.size)
        assertFalse(state.finished)
    }

    @Test
    fun `a reply is never sent and the response workflow is untouched`() = runTest {
        val viewModel = createViewModel()

        viewModel.reachLastBundledItem()

        assertTrue(responseWorkflow.requests.isEmpty())
        assertTrue(exerciseWorkflow.requests.isEmpty())
    }

    private companion object {
        const val MODULE_ID = "tones"
        val recordedAt: Instant = Instant.parse("2026-09-13T09:00:00Z")
        val fixedClock: Clock = Clock.fixed(recordedAt, ZoneOffset.UTC)
        val attemptFile = File("build/tmp/session-extra-practice-test/attempt.wav")

        val ma1 = item("tones-ma1", "ma1", listOf(1))
        val ma2 = item("tones-ma2", "ma2", listOf(2))

        val itemsById: Map<String, ContentItem> = listOf(ma1, ma2).associateBy { it.id }

        val lessonSpec = LessonSpec(
            id = "tones-01-first-tone",
            moduleId = MODULE_ID,
            title = "First tone",
            level = "beginner",
            topic = "tone contours",
            contentItemIds = listOf(ma1.id, ma2.id),
        )

        val listenAndChooseSpec = PracticeSpec(
            id = "tones-practice-listen-and-choose",
            moduleId = MODULE_ID,
            title = "Choose the pinyin",
            contentItemIds = listOf(ma1.id, ma2.id),
            mode = DrillMode.LISTEN_AND_CHOOSE,
        )

        fun item(id: String, pinyin: String, targetTones: List<Int>): ContentItem = ContentItem(
            id = id,
            type = ContentItemType.WORD,
            source = ContentSource.BUNDLED,
            meaning = pinyin,
            audioAssetRef = "reference/tones/$id.ogg",
            pinyin = pinyin,
            targetTones = targetTones,
        )
    }
}
