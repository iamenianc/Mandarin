package com.learnhuayu.core.ai

import kotlinx.serialization.Serializable

interface ConversationTurnWorkflow {
    suspend fun respond(request: ConversationTurnRequest): WorkflowResult<ConversationTurnReply>
}

data class ConversationTurnRequest(
    val scenario: String,
    val audio: AudioClip,
    val conversationState: ConversationState? = null,
    val targetDifficulty: String? = null,
)

@Serializable
data class ConversationTurnReply(
    val replyText: String,
    val gentleCorrection: String? = null,
    val nextPrompt: String? = null,
)
