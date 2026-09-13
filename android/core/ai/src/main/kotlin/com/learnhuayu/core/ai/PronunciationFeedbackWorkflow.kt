package com.learnhuayu.core.ai

import kotlinx.serialization.Serializable

interface PronunciationFeedbackWorkflow {
    suspend fun evaluate(request: PronunciationFeedbackRequest): WorkflowResult<PronunciationFeedback>
}

data class PronunciationFeedbackRequest(
    val pinyin: String,
    val referenceAudio: AudioClip,
    val attemptAudio: AudioClip,
    val targetTones: List<Int> = emptyList(),
    val learnerLevel: String? = null,
    val acousticEvidence: AcousticEvidence? = null,
)

@Serializable
data class PronunciationFeedback(
    val weakestUnit: String,
    val issue: String,
    val tip: String,
    val encouragement: String,
    val replayHint: String,
)
