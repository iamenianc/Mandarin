package com.learnhuayu.core.ai

import kotlinx.serialization.Serializable

interface ResponseTranscriptionWorkflow {
    suspend fun transcribe(request: ResponseTranscriptionRequest): WorkflowResult<ResponseTranscription>
}

data class ResponseTranscriptionRequest(
    val audio: AudioClip,
    val expectedOptions: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
)

@Serializable
data class ResponseTranscription(
    val transcript: String,
    val matchedOptionId: String? = null,
    val confidence: Double? = null,
)
