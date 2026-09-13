package com.learnhuayu.core.ai

interface SpeechSynthesisWorkflow {
    suspend fun synthesize(request: SpeechSynthesisRequest): WorkflowResult<SynthesizedSpeech>
}

sealed interface SpeechSynthesisRequest {
    val voice: String?
    val language: String?

    data class Pinyin(
        val pinyin: String,
        override val voice: String? = null,
        override val language: String? = null,
    ) : SpeechSynthesisRequest

    data class ReplyText(
        val replyText: String,
        override val voice: String? = null,
        override val language: String? = null,
    ) : SpeechSynthesisRequest
}

data class SynthesizedSpeech(
    val bytes: ByteArray,
    val contentType: String,
)
