package com.learnhuayu.core.ai

import com.learnhuayu.core.model.ScriptTurn
import kotlinx.serialization.Serializable

interface FieldMissionGenerationWorkflow {
    suspend fun generate(request: FieldMissionGenerationRequest): WorkflowResult<GeneratedMission>
}

data class FieldMissionGenerationRequest(
    val theme: String,
    val moduleContext: String? = null,
    val coveredContent: List<String> = emptyList(),
    val learnerLevel: String? = null,
    val debriefThemes: List<String> = emptyList(),
    val exchangeLength: Int? = null,
)

data class GeneratedMission(
    val script: List<ScriptTurn>,
    val locals: List<MissionLocal>,
)

@Serializable
data class MissionLocal(
    val label: String,
    val settingRole: String,
    val personality: String,
    val voiceProfile: String,
    val pace: String,
)
