package com.learnhuayu.app.ui.session

import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.app.ui.audio.FakeAudioPlayer
import com.learnhuayu.app.ui.audio.FakeAudioRecorder
import com.learnhuayu.app.ui.audio.FakeRecordingStore
import com.learnhuayu.app.ui.audio.FakeWavCodec
import com.learnhuayu.app.ui.audio.MainDispatcherRule
import com.learnhuayu.core.ai.PronunciationFeedback
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.playback.AudioSource
import com.learnhuayu.core.audio.playback.PlaybackState
import com.learnhuayu.core.audio.playback.PlaybackStatus
import com.learnhuayu.core.data.prefs.UserPreferences
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import com.learnhuayu.core.model.Progress
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

class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val player = FakeAudioPlayer()
    private val recorder = FakeAudioRecorder()
    private val wavCodec = FakeWavCodec()
    private val recordingStore = FakeRecordingStore(attemptFile)
    private val progressRepository = FakeProgressRepository()
    private val attemptRepository = FakeAttemptRepository()
    private val preferencesRepository = FakePreferencesRepository()
    private val feedbackWorkflow = FakePronunciationFeedbackWorkflow(
        WorkflowResult.Success(
            PronunciationFeedback(
                weakestUnit = "ma1",
                issue = "the tone is flat",
                tip = "start higher",
                encouragement = "good effort",
                replayHint = "listen to ma1 again",
            ),
        ),
    )
    private val referenceClipReader = FakeReferenceClipReader()
    private val evidenceSource = FakeAttemptEvidenceSource()
    private val responseWorkflow = FakeResponseTranscriptionWorkflow()
    private val contentRepository = FakeBundledContentRepository(items = itemsById)
    private val registry = ModuleRegistry(
        setOf(
            FakeLearningModule(
                id = MODULE_ID,
                title = "Tones",
                lessonSpecs = listOf(lessonSpec),
                practiceSpecs = listOf(hearAndNameSpec, listenAndChooseSpec, speakAndRepeatSpec),
            ),
        ),
    )

    private fun createViewModel(
        preferences: FakePreferencesRepository = preferencesRepository,
    ): SessionViewModel = SessionViewModel(
        registry = registry,
        contentRepository = contentRepository,
        audioPlayer = player,
        audioRecorder = recorder,
        wavCodec = wavCodec,
        recordingStore = recordingStore,
        progressRepository = progressRepository,
        attemptRepository = attemptRepository,
        preferencesRepository = preferences,
        feedbackWorkflow = feedbackWorkflow,
        responseWorkflow = responseWorkflow,
        referenceClipReader = referenceClipReader,
        evidenceSource = evidenceSource,
        clock = fixedClock,
    )

    @Test
    fun `lesson mode loads its items, plays the reference, and advances with progress`() = runTest {
        val viewModel = createViewModel()

        viewModel.load(MODULE_ID, "lesson", lessonSpec.id)

        assertEquals(DrillMode.LESSON, viewModel.uiState.value.mode)
        assertEquals(listOf(ma1, ma2), viewModel.uiState.value.items)
        assertEquals(ma1, viewModel.uiState.value.currentItem)
        assertTrue(viewModel.uiState.value.canAdvance)
        assertEquals(
            listOf(AudioSource.Asset("audio/reference/tones/tones-ma1.ogg")),
            player.playedSources,
        )

        viewModel.onNext()

        assertEquals(1, viewModel.uiState.value.index)
        assertEquals(listOf(ma1.id), progressRepository.upserts.map { it.contentItemId })
        assertEquals(1, progressRepository.upserts.single().timesPracticed)
        assertEquals(recordedAt, progressRepository.upserts.single().lastPracticedAt)
        assertEquals(ma2, viewModel.uiState.value.currentItem)

        viewModel.onNext()

        assertTrue(viewModel.uiState.value.finished)
        assertEquals(listOf(ma1.id, ma2.id), progressRepository.upserts.map { it.contentItemId })
    }

    @Test
    fun `advancing increments an existing practice count`() = runTest {
        progressRepository.seed(Progress(contentItemId = ma1.id, timesPracticed = 2))
        val viewModel = createViewModel()

        viewModel.load(MODULE_ID, "lesson", lessonSpec.id)
        viewModel.onNext()

        assertEquals(3, progressRepository.upserts.single().timesPracticed)
    }

    @Test
    fun `previous returns to the earlier lesson item without writing progress`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(MODULE_ID, "lesson", lessonSpec.id)
        viewModel.onNext()

        viewModel.onPrevious()

        assertEquals(0, viewModel.uiState.value.index)
        assertEquals(listOf(ma1.id), progressRepository.upserts.map { it.contentItemId })
    }

    @Test
    fun `hear and name offers deterministic tone-number choices`() = runTest {
        val viewModel = createViewModel()

        viewModel.load(MODULE_ID, "practice", hearAndNameSpec.id)

        assertEquals(DrillMode.HEAR_AND_NAME, viewModel.uiState.value.mode)
        assertEquals(listOf("1", "2", "3", "4"), viewModel.uiState.value.choices)
        assertFalse(viewModel.uiState.value.canAdvance)

        viewModel.onChoiceSelected("1")

        assertTrue(viewModel.uiState.value.answerRevealed)
        assertEquals(true, viewModel.uiState.value.answerCorrect)
        assertEquals("1", viewModel.uiState.value.correctAnswer)
        assertTrue(viewModel.uiState.value.canAdvance)
    }

    @Test
    fun `hear and name marks a wrong tone and still reveals the answer`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(MODULE_ID, "practice", hearAndNameSpec.id)

        viewModel.onChoiceSelected("2")

        assertTrue(viewModel.uiState.value.answerRevealed)
        assertEquals(false, viewModel.uiState.value.answerCorrect)
        assertEquals("1", viewModel.uiState.value.correctAnswer)
    }

    @Test
    fun `hear and name ignores a second choice after reveal`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(MODULE_ID, "practice", hearAndNameSpec.id)
        viewModel.onChoiceSelected("2")

        viewModel.onChoiceSelected("1")

        assertEquals("2", viewModel.uiState.value.selectedChoice)
        assertEquals(false, viewModel.uiState.value.answerCorrect)
    }

    @Test
    fun `listen and choose offers deterministic pinyin choices`() = runTest {
        val viewModel = createViewModel()

        viewModel.load(MODULE_ID, "practice", listenAndChooseSpec.id)

        assertEquals(DrillMode.LISTEN_AND_CHOOSE, viewModel.uiState.value.mode)
        assertEquals(listOf("ma1", "ma2", "ma3", "ma4"), viewModel.uiState.value.choices)
    }

    @Test
    fun `choice order is stable across a second load`() = runTest {
        val first = createViewModel()
        first.load(MODULE_ID, "practice", hearAndNameSpec.id)

        val second = createViewModel()
        second.load(MODULE_ID, "practice", hearAndNameSpec.id)

        assertEquals(first.uiState.value.choices, second.uiState.value.choices)
    }

    @Test
    fun `speak and repeat records, writes a wav, plays it back, and records an attempt`() = runTest {
        val viewModel = createViewModel()
        viewModel.onPermissionStatusChecked(true)
        viewModel.load(MODULE_ID, "practice", speakAndRepeatSpec.id)

        viewModel.onRecordClick()
        assertTrue(viewModel.uiState.value.isRecording)

        recorder.setLevel(0.5f)
        assertEquals(0.5f, viewModel.uiState.value.level, 0.0001f)

        viewModel.onRecordClick()

        assertEquals(attemptFile, wavCodec.writtenFile)
        assertEquals(listOf(SESSION_ATTEMPT_LABEL), recordingStore.requestedLabels)
        assertEquals(attemptFile.path, viewModel.uiState.value.attemptAudioRef)
        assertTrue(viewModel.uiState.value.attemptPlayback.hasPlayed)
        assertTrue(player.playedSources.contains(AudioSource.LocalFile(attemptFile.path)))

        viewModel.onNext()

        val attempt = attemptRepository.upserts.single()
        assertEquals(ma1.id, attempt.contentItemId)
        assertEquals(attemptFile.path, attempt.userAudioRef)
        assertEquals(recordedAt, attempt.recordedAt)
        assertEquals("attempt-${ma1.id}-${recordedAt.toEpochMilli()}", attempt.id)
        assertEquals(listOf(ma1.id), progressRepository.upserts.map { it.contentItemId })
    }

    @Test
    fun `speak and repeat does not record an attempt before the learner speaks`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(MODULE_ID, "practice", speakAndRepeatSpec.id)

        viewModel.onNext()

        assertTrue(attemptRepository.upserts.isEmpty())
        assertEquals(1, progressRepository.upserts.size)
    }

    @Test
    fun `recording asks for permission before starting the microphone`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(MODULE_ID, "practice", speakAndRepeatSpec.id)

        viewModel.onRecordClick()

        assertEquals(0, recorder.startCount)
        assertEquals(
            com.learnhuayu.app.ui.audio.MicrophonePermission.Requesting,
            viewModel.uiState.value.permission,
        )
    }

    @Test
    fun `a missing reference asset surfaces a message and still lets the learner continue`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(MODULE_ID, "lesson", lessonSpec.id)

        player.emit(
            PlaybackState(
                source = AudioSource.Asset("audio/reference/tones/tones-ma1.ogg"),
                status = PlaybackStatus.Failed("Source error"),
            ),
        )

        assertEquals("Source error", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.canAdvance)

        viewModel.onNext()
        assertEquals(1, viewModel.uiState.value.index)
    }

    @Test
    fun `recorder failure surfaces its message and state`() = runTest {
        val viewModel = createViewModel()
        viewModel.onPermissionStatusChecked(true)
        viewModel.load(MODULE_ID, "practice", speakAndRepeatSpec.id)

        recorder.fail("microphone unavailable")

        assertEquals("microphone unavailable", viewModel.uiState.value.recorderError)
        assertEquals(
            RecorderState.Failed("microphone unavailable"),
            viewModel.uiState.value.recorderState,
        )
    }

    @Test
    fun `leaving the screen stops an active recording`() = runTest {
        val viewModel = createViewModel()
        viewModel.onPermissionStatusChecked(true)
        viewModel.load(MODULE_ID, "practice", speakAndRepeatSpec.id)
        viewModel.onRecordClick()

        viewModel.onLeaveScreen()

        assertEquals(1, recorder.stopCount)
        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun `hangul aid follows the stored preference`() = runTest {
        val viewModel = createViewModel(
            FakePreferencesRepository(
                UserPreferences(
                    dailyGoalMinutes = null,
                    sessionLengthMinutes = null,
                    showHangul = true,
                    recordingsToProviderConsent = false,
                    consentVersion = "1",
                ),
            ),
        )

        viewModel.load(MODULE_ID, "lesson", lessonSpec.id)

        assertTrue(viewModel.uiState.value.showHangul)
    }

    @Test
    fun `an unknown practice spec is reported as not found`() = runTest {
        val viewModel = createViewModel()

        viewModel.load(MODULE_ID, "practice", "missing")

        assertFalse(viewModel.uiState.value.loading)
        assertTrue(viewModel.uiState.value.notFound)
    }

    @Test
    fun `an unknown session kind is reported as not found`() = runTest {
        val viewModel = createViewModel()

        viewModel.load(MODULE_ID, "quiz", lessonSpec.id)

        assertFalse(viewModel.uiState.value.loading)
        assertTrue(viewModel.uiState.value.notFound)
    }

    private companion object {
        const val MODULE_ID = "tones"
        val recordedAt: Instant = Instant.parse("2026-09-13T09:00:00Z")
        val fixedClock: Clock = Clock.fixed(recordedAt, ZoneOffset.UTC)
        val attemptFile = File("build/tmp/session-test/attempt.wav")

        val ma1 = item("tones-ma1", "ma1", listOf(1), "엄마")
        val ma2 = item("tones-ma2", "ma2", listOf(2))
        val ma3 = item("tones-ma3", "ma3", listOf(3))
        val ma4 = item("tones-ma4", "ma4", listOf(4))
        val ma5 = item("tones-ma5", "ma5", listOf(5))

        val itemsById: Map<String, ContentItem> = listOf(ma1, ma2, ma3, ma4, ma5).associateBy { it.id }

        val lessonSpec = LessonSpec(
            id = "tones-01-first-tone",
            moduleId = MODULE_ID,
            title = "First tone",
            level = "beginner",
            topic = "tone contours",
            contentItemIds = listOf(ma1.id, ma2.id),
        )

        val hearAndNameSpec = PracticeSpec(
            id = "tones-practice-hear-and-name",
            moduleId = MODULE_ID,
            title = "Hear and name the tone",
            contentItemIds = listOf(ma1.id, ma2.id, ma3.id, ma4.id, ma5.id),
            mode = DrillMode.HEAR_AND_NAME,
        )

        val listenAndChooseSpec = PracticeSpec(
            id = "tones-practice-listen-and-choose",
            moduleId = MODULE_ID,
            title = "Choose the pinyin",
            contentItemIds = listOf(ma1.id, ma2.id, ma3.id, ma4.id, ma5.id),
            mode = DrillMode.LISTEN_AND_CHOOSE,
        )

        val speakAndRepeatSpec = PracticeSpec(
            id = "tones-practice-say-and-repeat",
            moduleId = MODULE_ID,
            title = "Say and repeat the tone",
            contentItemIds = listOf(ma1.id, ma2.id),
            mode = DrillMode.SPEAK_AND_REPEAT,
        )

        fun item(
            id: String,
            pinyin: String,
            targetTones: List<Int>,
            hangul: String? = null,
        ): ContentItem = ContentItem(
            id = id,
            type = ContentItemType.WORD,
            source = ContentSource.BUNDLED,
            meaning = pinyin,
            audioAssetRef = "reference/tones/$id.ogg",
            pinyin = pinyin,
            hangul = hangul,
            targetTones = targetTones,
        )
    }
}
