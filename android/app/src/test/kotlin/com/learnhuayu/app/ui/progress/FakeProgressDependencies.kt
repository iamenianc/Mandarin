package com.learnhuayu.app.ui.progress

import com.learnhuayu.core.ai.ProgressSummary
import com.learnhuayu.core.ai.ProgressSummaryRequest
import com.learnhuayu.core.ai.ProgressSummaryWorkflow
import com.learnhuayu.core.ai.SpeechSynthesisRequest
import com.learnhuayu.core.ai.SpeechSynthesisWorkflow
import com.learnhuayu.core.ai.SynthesizedSpeech
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.data.repository.DebriefEntryRepository
import com.learnhuayu.core.model.DebriefEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeProgressSummaryWorkflow(
    private var result: WorkflowResult<ProgressSummary> = WorkflowResult.Success(
        ProgressSummary(summaryText = "placeholder", focusAreas = emptyList()),
    ),
) : ProgressSummaryWorkflow {

    val requests = mutableListOf<ProgressSummaryRequest>()

    fun returns(result: WorkflowResult<ProgressSummary>) {
        this.result = result
    }

    override suspend fun summarize(request: ProgressSummaryRequest): WorkflowResult<ProgressSummary> {
        requests += request
        return result
    }
}

class FakeSpeechSynthesisWorkflow(
    private var result: WorkflowResult<SynthesizedSpeech> = WorkflowResult.Failure(WorkflowFailure.BaseUrlMissing()),
) : SpeechSynthesisWorkflow {

    val requests = mutableListOf<SpeechSynthesisRequest>()

    fun returns(result: WorkflowResult<SynthesizedSpeech>) {
        this.result = result
    }

    override suspend fun synthesize(request: SpeechSynthesisRequest): WorkflowResult<SynthesizedSpeech> {
        requests += request
        return result
    }
}

class FakeProgressAudioPlayer : ProgressAudioPlayer {

    val played = mutableListOf<Pair<ByteArray, String?>>()

    override suspend fun playSpeech(bytes: ByteArray, contentType: String?) {
        played += bytes to contentType
    }
}

class FakeDebriefEntryRepository : DebriefEntryRepository {

    private val stored = MutableStateFlow<List<DebriefEntry>>(emptyList())

    fun seed(entries: List<DebriefEntry>) {
        stored.value = entries
    }

    override suspend fun upsert(entry: DebriefEntry) {
        stored.value = stored.value.filterNot { it.id == entry.id } + entry
    }

    override suspend fun byId(id: String): DebriefEntry? = stored.value.firstOrNull { it.id == id }

    override fun byMission(missionId: String): Flow<List<DebriefEntry>> = stored

    override fun all(): Flow<List<DebriefEntry>> = stored

    override suspend fun deleteById(id: String) {
        stored.value = stored.value.filterNot { it.id == id }
    }
}
