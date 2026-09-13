package com.learnhuayu.core.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ExerciseGenerationWorkflow {
    suspend fun generate(request: ExerciseGenerationRequest): WorkflowResult<GeneratedExercises>
}

data class ExerciseGenerationRequest(
    val moduleId: String,
    val itemType: ExerciseItemType,
    val theme: String? = null,
    val targetUnits: List<String> = emptyList(),
    val difficulty: String? = null,
    val feedbackThemes: List<String> = emptyList(),
    val count: Int? = null,
)

@Serializable
enum class ExerciseItemType {
    @SerialName("word")
    WORD,

    @SerialName("phrase")
    PHRASE,

    @SerialName("minimalPair")
    MINIMAL_PAIR,

    @SerialName("dialogue")
    DIALOGUE,
}

@Serializable
data class GeneratedExercises(
    val items: List<GeneratedExercise> = emptyList(),
)

@Serializable
data class GeneratedExercise(
    val type: ExerciseItemType,
    val meaning: String,
    val pinyin: String,
    val targetTones: List<Int>,
    val rationale: String,
    val distractors: List<String> = emptyList(),
    val hangul: String? = null,
)
