package com.learnhuayu.core.ai

import kotlinx.serialization.Serializable

interface ProgressSummaryWorkflow {
    suspend fun summarize(request: ProgressSummaryRequest): WorkflowResult<ProgressSummary>
}

data class ProgressSummaryRequest(
    val attemptCounts: Map<String, Int>,
    val feedbackThemes: List<String>,
    val moduleIds: List<String> = emptyList(),
    val lessonIds: List<String> = emptyList(),
    val debriefThemes: List<String> = emptyList(),
)

@Serializable
data class ProgressSummary(
    val summaryText: String,
    val focusAreas: List<String> = emptyList(),
)
