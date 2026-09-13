package com.learnhuayu.core.ai

import kotlinx.serialization.Serializable

@Serializable
data class ConversationState(
    val transcripts: List<String> = emptyList(),
    val corrections: List<String> = emptyList(),
)
