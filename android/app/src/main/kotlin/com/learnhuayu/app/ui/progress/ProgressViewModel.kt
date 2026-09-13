package com.learnhuayu.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnhuayu.core.ai.ProgressSummaryRequest
import com.learnhuayu.core.ai.ProgressSummaryWorkflow
import com.learnhuayu.core.ai.SpeechSynthesisRequest
import com.learnhuayu.core.ai.SpeechSynthesisWorkflow
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.data.repository.AttemptRepository
import com.learnhuayu.core.data.repository.DebriefEntryRepository
import com.learnhuayu.core.data.repository.ProgressRepository
import com.learnhuayu.core.model.Attempt
import com.learnhuayu.core.model.DebriefEntry
import com.learnhuayu.core.model.DebriefKind
import com.learnhuayu.core.model.Progress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Friendly note shown when WF-5 is unavailable; the local themes are still useful. */
const val PROGRESS_OFFLINE_MESSAGE = "The online summary is unavailable right now. Your local practice themes are shown below."

/** One practised content item, with its pinyin resolved from the bundled corpus. */
data class PractisedItem(
    val contentItemId: String,
    val pinyin: String,
    val meaning: String,
    val timesPracticed: Int,
)

/**
 * Everything the progress screen renders. [summaryText] and [focusAreas] come from WF-5 when
 * it succeeds; [feedbackThemes], [debriefThemes], and [mostPractised] are always computed
 * locally, so the screen is useful offline. [message] carries the friendly WF-5 fallback note.
 */
data class ProgressUiState(
    val loading: Boolean = true,
    val hasHistory: Boolean = false,
    val summaryText: String? = null,
    val focusAreas: List<String> = emptyList(),
    val feedbackThemes: List<String> = emptyList(),
    val debriefThemes: List<String> = emptyList(),
    val mostPractised: List<PractisedItem> = emptyList(),
    val message: String? = null,
    val playing: Boolean = false,
) {
    val canPlaySummary: Boolean
        get() = summaryText != null && !playing
}

/**
 * Reports practice history and recurring problems (M5, FR-34, WF-5). It reads local progress,
 * attempts, and debrief entries, always computes local themes and the most-practised items, and
 * asks WF-5 for a short spoken summary from the aggregated metadata only (no audio, no
 * transcripts). A workflow failure never crashes and never hides the local view. The summary is
 * voiced through WF-3 when the learner taps play, and a TTS failure is silent.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val attemptRepository: AttemptRepository,
    private val debriefRepository: DebriefEntryRepository,
    private val contentRepository: BundledContentRepository,
    private val summaryWorkflow: ProgressSummaryWorkflow,
    private val speechWorkflow: SpeechSynthesisWorkflow,
    private val audioPlayer: ProgressAudioPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads local history and asks WF-5 for a fresh summary when there is any history. */
    fun refresh() {
        _uiState.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            val progress = runWorkflow { progressRepository.all().first() } ?: emptyList()
            val attempts = runWorkflow { attemptRepository.all().first() } ?: emptyList()
            val debriefEntries = runWorkflow { debriefRepository.all().first() } ?: emptyList()
            applyHistory(progress, attempts, debriefEntries)
        }
    }

    /** Voices [ProgressUiState.summaryText] through WF-3; a failure or empty summary is silent. */
    fun onPlaySummary() {
        val text = _uiState.value.summaryText ?: return
        if (_uiState.value.playing) return
        _uiState.update { it.copy(playing = true) }
        viewModelScope.launch {
            val result = runWorkflow { speechWorkflow.synthesize(SpeechSynthesisRequest.ReplyText(replyText = text)) }
            val speech = (result as? WorkflowResult.Success)?.value
            if (speech != null) {
                runWorkflow { audioPlayer.playSpeech(speech.bytes, speech.contentType) }
            }
            _uiState.update { it.copy(playing = false) }
        }
    }

    private suspend fun applyHistory(
        progress: List<Progress>,
        attempts: List<Attempt>,
        debriefEntries: List<DebriefEntry>,
    ) {
        val hasHistory = progress.isNotEmpty() || attempts.isNotEmpty() || debriefEntries.isNotEmpty()
        if (!hasHistory) {
            _uiState.value = ProgressUiState(loading = false, hasHistory = false)
            return
        }
        val feedbackThemes = recurringThemes(progress.flatMap { it.feedbackThemes })
        val debriefThemes = recurringDebriefThemes(debriefEntries)
        _uiState.update {
            it.copy(
                loading = false,
                hasHistory = true,
                feedbackThemes = feedbackThemes,
                debriefThemes = debriefThemes,
                mostPractised = mostPractised(progress),
            )
        }

        val practisedIds = (progress.map { it.contentItemId } + attempts.map { it.contentItemId }).toSet()
        val (moduleIds, lessonIds) = moduleAndLessonIds(practisedIds)
        val request = ProgressSummaryRequest(
            attemptCounts = attempts.groupingBy { it.contentItemId }.eachCount(),
            feedbackThemes = feedbackThemes,
            moduleIds = moduleIds,
            lessonIds = lessonIds,
            debriefThemes = debriefThemes,
        )
        val result = runWorkflow { summaryWorkflow.summarize(request) }
        when (result) {
            is WorkflowResult.Success -> _uiState.update {
                it.copy(summaryText = result.value.summaryText, focusAreas = result.value.focusAreas)
            }

            else -> _uiState.update { it.copy(message = PROGRESS_OFFLINE_MESSAGE) }
        }
    }

    private suspend fun mostPractised(progress: List<Progress>): List<PractisedItem> = progress
        .sortedWith(compareByDescending<Progress> { it.timesPracticed }.thenBy { it.contentItemId })
        .take(MAX_ITEMS)
        .map { row ->
            val item = runWorkflow { contentRepository.contentItem(row.contentItemId) }
            PractisedItem(
                contentItemId = row.contentItemId,
                pinyin = item?.pinyin ?: row.contentItemId,
                meaning = item?.meaning.orEmpty(),
                timesPracticed = row.timesPracticed,
            )
        }

    private suspend fun moduleAndLessonIds(contentItemIds: Set<String>): Pair<List<String>, List<String>> {
        if (contentItemIds.isEmpty()) return emptyList<String>() to emptyList()
        val modules = try {
            contentRepository.modules()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            emptyList()
        }
        val moduleIds = linkedSetOf<String>()
        val lessonIds = linkedSetOf<String>()
        modules.forEach { module ->
            module.lessonSpecs.forEach { lesson ->
                if (lesson.contentItemIds.any { it in contentItemIds }) {
                    moduleIds += module.id
                    lessonIds += lesson.id
                }
            }
            module.practiceSpecs.forEach { practice ->
                if (practice.contentItemIds.any { it in contentItemIds }) moduleIds += module.id
            }
        }
        return moduleIds.toList() to lessonIds.toList()
    }

    private fun recurringThemes(values: List<String>): List<String> = values
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }
        .take(MAX_THEMES)

    private fun recurringDebriefThemes(entries: List<DebriefEntry>): List<String> = entries
        .map { entry -> "${entry.pinyin.trim()} (${entry.kind.label()})" }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }
        .take(MAX_THEMES)

    /** Runs a suspend call, returning null on any failure so no path can crash the screen. */
    private suspend fun <T> runWorkflow(call: suspend () -> T): T? = try {
        call()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        null
    }

    private fun DebriefKind.label(): String = when (this) {
        DebriefKind.UNFAMILIAR_WORD -> "unfamiliar word"
        DebriefKind.TONE_BREAKDOWN -> "tone breakdown"
    }

    private companion object {
        const val MAX_THEMES = 5
        const val MAX_ITEMS = 5
    }
}
