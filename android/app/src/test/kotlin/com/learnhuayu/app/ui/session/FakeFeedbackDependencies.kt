package com.learnhuayu.app.ui.session

import com.learnhuayu.app.audio.ReferencePcmDecoder
import com.learnhuayu.core.ai.AcousticEvidence
import com.learnhuayu.core.ai.AudioClip
import com.learnhuayu.core.ai.PronunciationFeedback
import com.learnhuayu.core.ai.PronunciationFeedbackRequest
import com.learnhuayu.core.ai.PronunciationFeedbackWorkflow
import com.learnhuayu.core.ai.ResponseTranscription
import com.learnhuayu.core.ai.ResponseTranscriptionRequest
import com.learnhuayu.core.ai.ResponseTranscriptionWorkflow
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.model.ContentItem

class FakePronunciationFeedbackWorkflow(
    private var result: WorkflowResult<PronunciationFeedback>,
) : PronunciationFeedbackWorkflow {

    val requests = mutableListOf<PronunciationFeedbackRequest>()

    fun returns(result: WorkflowResult<PronunciationFeedback>) {
        this.result = result
    }

    override suspend fun evaluate(request: PronunciationFeedbackRequest): WorkflowResult<PronunciationFeedback> {
        requests += request
        return result
    }
}

class FakeResponseTranscriptionWorkflow(
    private var result: WorkflowResult<ResponseTranscription> =
        WorkflowResult.Failure(WorkflowFailure.BaseUrlMissing()),
) : ResponseTranscriptionWorkflow {

    val requests = mutableListOf<ResponseTranscriptionRequest>()

    fun returns(result: WorkflowResult<ResponseTranscription>) {
        this.result = result
    }

    override suspend fun transcribe(request: ResponseTranscriptionRequest): WorkflowResult<ResponseTranscription> {
        requests += request
        return result
    }
}

class FakeReferenceClipReader(
    private val clips: Map<String, AudioClip> = emptyMap(),
) : ReferenceClipReader {

    val requestedPaths = mutableListOf<String>()

    override suspend fun read(audioAssetPath: String): AudioClip? {
        requestedPaths += audioAssetPath
        return clips[audioAssetPath]
    }
}

class FakeReferencePcmDecoder(
    private var result: PcmAudio? = null,
) : ReferencePcmDecoder {

    var decodeCount = 0
        private set

    val decodedClips = mutableListOf<AudioClip>()

    fun returns(result: PcmAudio?) {
        this.result = result
    }

    override suspend fun decode(clip: AudioClip): PcmAudio? {
        decodeCount++
        decodedClips += clip
        return result
    }
}

class FakeAttemptEvidenceSource(
    private var evidence: AcousticEvidence? = null,
) : AttemptEvidenceSource {

    val calls = mutableListOf<Pair<ContentItem, PcmAudio>>()

    fun returns(evidence: AcousticEvidence?) {
        this.evidence = evidence
    }

    override suspend fun evidenceFor(item: ContentItem, attempt: PcmAudio): AcousticEvidence? {
        calls += item to attempt
        return evidence
    }
}
