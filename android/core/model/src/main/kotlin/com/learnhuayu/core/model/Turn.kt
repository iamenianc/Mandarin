package com.learnhuayu.core.model

data class Turn(
    val id: String,
    val sessionId: String,
    val speaker: ConversationSpeaker,
    val audioRef: String? = null,
    val transcript: String? = null,
    val feedback: String? = null,
)

enum class ConversationSpeaker {
    USER,
    AI,
}
