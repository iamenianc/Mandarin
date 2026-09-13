package com.learnhuayu.core.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface MandarinQaWorkflow {
    suspend fun ask(request: MandarinQaRequest): WorkflowResult<MandarinQaAnswer>
}

data class MandarinQaRequest(
    val question: String? = null,
    val audio: AudioClip? = null,
    val history: List<QaExchange> = emptyList(),
    val learnerLevel: String? = null,
) {
    init {
        require(question != null || audio != null) {
            "a text question or a spoken question is required"
        }
    }
}

@Serializable
data class QaExchange(
    val role: RaymondRole,
    val text: String,
)

@Serializable
enum class RaymondRole {
    @SerialName("user")
    USER,

    @SerialName("raymond")
    RAYMOND,
}

@Serializable
data class MandarinQaAnswer(
    val answerText: String,
    val examples: List<MandarinExample> = emptyList(),
    val followUps: List<String> = emptyList(),
)

@Serializable
data class MandarinExample(
    val pinyin: String,
    val meaning: String,
)
