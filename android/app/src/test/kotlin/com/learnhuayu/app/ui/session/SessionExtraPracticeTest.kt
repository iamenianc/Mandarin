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
import com.learnhuayu.core.ai.SpeechSynthesisRequest
import com.learnhuayu.core.ai.SpeechSynthesisWorkflow
import com.learnhuayu.core.ai.SynthesizedSpeech
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.playback.AudioSource
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
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
    private val speechWorkflow = FakeSpeechWorkflow()
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
        speechWorkflow = speechWorkflow,
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

    @Test
    fun `a generated item voices its pinyin and replays the cached clip`() = runTest {
        speechWorkflow.returns(WorkflowResult.Success(SynthesizedSpeech(bytes = ttsBytes, contentType = "audio/wav")))
        exerciseWorkflow.returns(WorkflowResult.Success(GeneratedExercises(items = listOf(ma3Exercise))))
        player.playedSources.clear()
        recordingStore.requestedLabels.clear()
        val viewModel = createViewModel()
        viewModel.reachLastBundledItem()
        player.playedSources.clear()
        viewModel.onKeepPractisingClick()

        viewModel.onChoiceSelected("ma2")
        viewModel.onNext()

        val extra = viewModel.uiState.value.currentItem
        assertEquals(ContentSource.GENERATED, extra?.source)
        assertEquals(SpeechSynthesisRequest.Pinyin(pinyin = "ma3"), speechWorkflow.requests.single())
        assertEquals(listOf(GENERATED_AUDIO_LABEL), recordingStore.requestedLabels)
        val extraClip = player.playedSources.filterIsInstance<AudioSource.LocalFile>().singleOrNull()
        assertEquals(
            "expected one cached generated clip, was: ${player.playedSources}",
            File(attemptFile.path).absolutePath,
            extraClip?.path?.let { File(it).absolutePath },
        )
        val requestsAfterFirstPlay = speechWorkflow.requests.size

        viewModel.onPlayReferenceClick()

        assertEquals(requestsAfterFirstPlay, speechWorkflow.requests.size)
        val replayed = player.playedSources.filterIsInstance<AudioSource.LocalFile>()
        assertTrue(
            "expected the cached clip replayed last, was: $replayed",
            replayed.isNotEmpty() && replayed.last() == extraClip,
        )
        viewModel.onChoiceSelected("ma3")

        assertTrue(viewModel.uiState.value.canAdvance)
    }

    @Test
    fun `a 501 tts failure keeps the missing-clip behavior and stays finishable`() = runTest {
        speechWorkflow.returns(WorkflowResult.Failure(WorkflowFailure.ProviderNotConfigured("tts is not configured")))
        exerciseWorkflow.returns(WorkflowResult.Success(GeneratedExercises(items = listOf(ma3Exercise))))
        player.playedSources.clear()
        val viewModel = createViewModel()
        viewModel.reachLastBundledItem()
        player.playedSources.clear()
        viewModel.onKeepPractisingClick()

        viewModel.onChoiceSelected("ma2")
        viewModel.onNext()

        val extra = viewModel.uiState.value.currentItem
        assertEquals(ContentSource.GENERATED, extra?.source)
        assertEquals(listOf(SpeechSynthesisRequest.Pinyin(pinyin = "ma3")), speechWorkflow.requests)
        assertTrue(
            "expected the missing-clip asset, was: ${player.playedSources}",
            player.playedSources.contains(AudioSource.Asset("audio/")),
        )

        viewModel.onPlayReferenceClick()
        viewModel.onChoiceSelected("ma3")
        viewModel.onNext()

        assertTrue(viewModel.uiState.value.finished)
        assertTrue(viewModel.uiState.value.canAdvance)
    }

    @Test
    fun `a thrown tts failure never crashes and the session continues`() = runTest {
        speechWorkflow.throws(IllegalStateException("offline"))
        exerciseWorkflow.returns(WorkflowResult.Success(GeneratedExercises(items = listOf(ma3Exercise))))
        player.playedSources.clear()
        val viewModel = createViewModel()
        viewModel.reachLastBundledItem()
        viewModel.onKeepPractisingClick()

        viewModel.onChoiceSelected("ma2")
        viewModel.onNext()

        assertEquals(ContentSource.GENERATED, viewModel.uiState.value.currentItem?.source)
        assertEquals(1, speechWorkflow.requests.size)

        viewModel.onPlayReferenceClick()
        viewModel.onChoiceSelected("ma3")
        viewModel.onNext()

        assertTrue(viewModel.uiState.value.finished)
    }

    @Test
    fun `feedback uses the cached generated clip as the reference`() = runTest {
        speechWorkflow.returns(WorkflowResult.Success(SynthesizedSpeech(bytes = ttsBytes, contentType = "audio/wav")))
        exerciseWorkflow.returns(WorkflowResult.Success(GeneratedExercises(items = listOf(ma3Exercise))))
        val reader = FakeReferenceClipReader()
        val feedback = FakePronunciationFeedbackWorkflow(feedbackWorkflowResult())
        val speakViewModel = SessionViewModel(
            registry = feedbackRegistry,
            contentRepository = contentRepository,
            audioPlayer = player,
            audioRecorder = recorder,
            wavCodec = wavCodec,
            recordingStore = recordingStore,
            progressRepository = progressRepository,
            attemptRepository = attemptRepository,
            preferencesRepository = FakePreferencesRepository(),
            feedbackWorkflow = feedback,
            responseWorkflow = responseWorkflow,
            exerciseWorkflow = exerciseWorkflow,
            referenceClipReader = reader,
            evidenceSource = FakeAttemptEvidenceSource(),
            speechWorkflow = speechWorkflow,
            clock = fixedClock,
        )
        speakViewModel.load(MODULE_ID, "practice", feedbackPracticeSpec.id)
        speakViewModel.onNext()
        speakViewModel.onNext()
        speakViewModel.onKeepPractisingClick()
        speakViewModel.onNext()

        speakViewModel.onPermissionStatusChecked(true)
        speakViewModel.onRecordClick()
        speakViewModel.onRecordClick()
        speakViewModel.onGetFeedbackClick()

        val request = feedback.requests.single()
        assertEquals("ma3", request.pinyin)
        assertArrayEquals(ttsBytes, request.referenceAudio.bytes)
        assertEquals(com.learnhuayu.core.ai.AudioFormat.WAV, request.referenceAudio.format)
        assertTrue(reader.requestedPaths.isEmpty())
        assertEquals(FeedbackUiState.Available(feedbackWorkflowResult().value), speakViewModel.uiState.value.feedback)
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

        val ma3Exercise = GeneratedExercise(
            type = ExerciseItemType.WORD,
            meaning = "horse",
            pinyin = "ma3",
            targetTones = listOf(3),
            rationale = "same syllable family",
        )

        val ttsBytes = byteArrayOf(9, 8, 7, 6)

        fun feedbackWorkflowResult(): WorkflowResult.Success<PronunciationFeedback> = WorkflowResult.Success(
            PronunciationFeedback(
                weakestUnit = "ma1",
                issue = "the tone is flat",
                tip = "start higher",
                encouragement = "good effort",
                replayHint = "listen to ma1 again",
            ),
        )

        val feedbackPracticeSpec = PracticeSpec(
            id = "tones-practice-say-and-repeat",
            moduleId = MODULE_ID,
            title = "Say and repeat the tone",
            contentItemIds = listOf(ma1.id, ma2.id),
            mode = DrillMode.SPEAK_AND_REPEAT_FEEDBACK,
        )

        val feedbackRegistry = ModuleRegistry(
            setOf(
                FakeLearningModule(
                    id = MODULE_ID,
                    title = "Tones",
                    lessonSpecs = listOf(lessonSpec),
                    practiceSpecs = listOf(listenAndChooseSpec, feedbackPracticeSpec),
                ),
            ),
        )
    }

    class FakeSpeechWorkflow(
        private var result: WorkflowResult<SynthesizedSpeech> =
            WorkflowResult.Failure(WorkflowFailure.BaseUrlMissing()),
        private var failure: Throwable? = null,
    ) : SpeechSynthesisWorkflow {

        val requests = mutableListOf<SpeechSynthesisRequest>()

        fun returns(result: WorkflowResult<SynthesizedSpeech>) {
            this.result = result
            this.failure = null
        }

        fun throws(failure: Throwable) {
            this.failure = failure
        }

        override suspend fun synthesize(request: SpeechSynthesisRequest): WorkflowResult<SynthesizedSpeech> {
            requests += request
            failure?.let { throw it }
            return result
        }
    }
}
