package com.learnhuayu.core.ai

import com.learnhuayu.core.model.ScriptTurn
import kotlinx.serialization.Serializable

interface LocalTurnWorkflow {
    suspend fun respond(request: LocalTurnRequest): WorkflowResult<LocalTurnReply>
}

data class LocalTurnRequest(
    val persona: MissionLocal,
    val mission: LocalTurnMission,
    val audio: AudioClip,
    val conversationState: ConversationState? = null,
    val targetDifficulty: String? = null,
)

data class LocalTurnMission(
    val script: List<ScriptTurn>,
    val goal: String? = null,
)

@Serializable
data class LocalTurnReply(
    val replyText: String,
    val understandingSignal: String? = null,
    val nextLocalPrompt: String? = null,
)
