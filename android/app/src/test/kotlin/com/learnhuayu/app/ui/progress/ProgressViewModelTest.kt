package com.learnhuayu.app.ui.progress

import com.learnhuayu.app.ui.audio.MainDispatcherRule
import com.learnhuayu.app.ui.session.FakeAttemptRepository
import com.learnhuayu.app.ui.session.FakeBundledContentRepository
import com.learnhuayu.app.ui.session.FakeProgressRepository
import com.learnhuayu.core.ai.ProgressSummary
import com.learnhuayu.core.ai.SynthesizedSpeech
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.data.content.ContentModule
import com.learnhuayu.core.model.Attempt
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.DebriefEntry
import com.learnhuayu.core.model.DebriefKind
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.Progress
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Progress reporting (M5, FR-34, WF-5). The view model always computes local themes and the
 * most-practised items; WF-5 only adds the spoken summary. The tests cover a successful
 * summary, the offline/local fallback when WF-5 fails, and the empty-history state.
 */
class ProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val progressRepository = FakeProgressRepository()
    private val attemptRepository = FakeAttemptRepository()
    private val debriefRepository = FakeDebriefEntryRepository()
    private val contentRepository = FakeBundledContentRepository(
        contentModules = listOf(contentModule),
        items = itemsById,
    )
    private val summaryWorkflow = FakeProgressSummaryWorkflow()
    private val speechWorkflow = FakeSpeechSynthesisWorkflow()
    private val audioPlayer = FakeProgressAudioPlayer()

    private fun createViewModel(): ProgressViewModel = ProgressViewModel(
        progressRepository = progressRepository,
        attemptRepository = attemptRepository,
        debriefRepository = debriefRepository,
        contentRepository = contentRepository,
        summaryWorkflow = summaryWorkflow,
        speechWorkflow = speechWorkflow,
        audioPlayer = audioPlayer,
    )

    private suspend fun seedHistory() {
        progressRepository.seed(
            Progress(
                contentItemId = ma1.id,
                timesPracticed = 5,
                lastPracticedAt = NOW,
                feedbackThemes = listOf("third tone", "final -ng"),
            ),
        )
        progressRepository.seed(
            Progress(
                contentItemId = ma2.id,
                timesPracticed = 2,
                lastPracticedAt = NOW,
                feedbackThemes = listOf("third tone"),
            ),
        )
        attemptRepository.upsert(attempt("attempt-1", ma1.id))
        attemptRepository.upsert(attempt("attempt-2", ma1.id))
        attemptRepository.upsert(attempt("attempt-3", ma2.id))
        debriefRepository.seed(
            listOf(
                debrief("debrief-1", "ma2", DebriefKind.TONE_BREAKDOWN),
                debrief("debrief-2", "ma2", DebriefKind.TONE_BREAKDOWN),
                debrief("debrief-3", "ma3", DebriefKind.UNFAMILIAR_WORD),
            ),
        )
    }

    @Test
    fun `successful summary renders WF-5 text plus local themes and counts`() = runTest {
        summaryWorkflow.returns(
            WorkflowResult.Success(
                ProgressSummary(
                    summaryText = "five attempts on the greeting this week",
                    focusAreas = listOf("third tone", "rhythm"),
                ),
            ),
        )
        seedHistory()

        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.loading)
        assertTrue(state.hasHistory)
        assertEquals("five attempts on the greeting this week", state.summaryText)
        assertEquals(listOf("third tone", "rhythm"), state.focusAreas)
        assertNull(state.message)
        assertEquals(listOf("third tone", "final -ng"), state.feedbackThemes)
        assertEquals("ma2 (tone breakdown)", state.debriefThemes.first())
        assertEquals("ma1", state.mostPractised.first().pinyin)
        assertEquals("mother", state.mostPractised.first().meaning)
        assertEquals(5, state.mostPractised.first().timesPracticed)

        val request = summaryWorkflow.requests.single()
        assertEquals(mapOf(ma1.id to 2, ma2.id to 1), request.attemptCounts)
        assertEquals(listOf("tones"), request.moduleIds)
        assertEquals(listOf("tones-1"), request.lessonIds)
        assertEquals(listOf("third tone", "final -ng"), request.feedbackThemes)
        assertEquals(state.debriefThemes, request.debriefThemes)
    }

    @Test
    fun `WF-5 failure keeps local themes and shows a friendly message`() = runTest {
        summaryWorkflow.returns(WorkflowResult.Failure(WorkflowFailure.BaseUrlMissing()))
        seedHistory()

        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.loading)
        assertTrue(state.hasHistory)
        assertNull(state.summaryText)
        assertEquals(PROGRESS_OFFLINE_MESSAGE, state.message)
        assertEquals(listOf("third tone", "final -ng"), state.feedbackThemes)
        assertEquals("ma1", state.mostPractised.first().pinyin)
        assertEquals(5, state.mostPractised.first().timesPracticed)
    }

    @Test
    fun `empty history shows the empty state and does not call WF-5`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.loading)
        assertFalse(state.hasHistory)
        assertNull(state.summaryText)
        assertNull(state.message)
        assertTrue(state.mostPractised.isEmpty())
        assertTrue(state.feedbackThemes.isEmpty())
        assertTrue(state.debriefThemes.isEmpty())
        assertTrue(summaryWorkflow.requests.isEmpty())
    }

    @Test
    fun `play summary voices the summary and stays silent when speech fails`() = runTest {
        summaryWorkflow.returns(WorkflowResult.Success(ProgressSummary(summaryText = "well done")))
        seedHistory()
        speechWorkflow.returns(WorkflowResult.Success(SynthesizedSpeech(bytes = byteArrayOf(1, 2, 3), contentType = "audio/wav")))

        val voiced = createViewModel()
        voiced.onPlaySummary()

        assertEquals(1, audioPlayer.played.size)
        assertEquals(byteArrayOf(1, 2, 3).toList(), audioPlayer.played.single().first.toList())
        assertFalse(voiced.uiState.value.playing)

        speechWorkflow.returns(WorkflowResult.Failure(WorkflowFailure.BaseUrlMissing()))
        val silent = createViewModel()
        silent.onPlaySummary()

        assertEquals(1, audioPlayer.played.size)
        assertFalse(silent.uiState.value.playing)
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-14T10:00:00Z")

        val ma1 = ContentItem(
            id = "phrase-ma1",
            type = ContentItemType.WORD,
            source = ContentSource.BUNDLED,
            meaning = "mother",
            audioAssetRef = "tones/ma1.ogg",
            pinyin = "ma1",
            targetTones = listOf(1),
        )

        val ma2 = ContentItem(
            id = "phrase-ma2",
            type = ContentItemType.WORD,
            source = ContentSource.BUNDLED,
            meaning = "hemp",
            audioAssetRef = "tones/ma2.ogg",
            pinyin = "ma2",
            targetTones = listOf(2),
        )

        val itemsById = mapOf(ma1.id to ma1, ma2.id to ma2)

        val contentModule = ContentModule(
            id = "tones",
            title = "Tones",
            theme = "tones",
            lessonSpecs = listOf(
                LessonSpec(
                    id = "tones-1",
                    moduleId = "tones",
                    title = "Tone one",
                    level = "beginner",
                    topic = "tones",
                    contentItemIds = listOf(ma1.id),
                ),
            ),
            practiceSpecs = emptyList(),
        )

        fun attempt(id: String, contentItemId: String) = Attempt(
            id = id,
            contentItemId = contentItemId,
            recordedAt = NOW,
            userAudioRef = "recordings/$id.wav",
            transcript = null,
            feedbackText = null,
        )

        fun debrief(id: String, pinyin: String, kind: DebriefKind) = DebriefEntry(
            id = id,
            missionId = "mission-1",
            pinyin = pinyin,
            kind = kind,
            note = null,
            createdAt = NOW,
        )
    }
}
