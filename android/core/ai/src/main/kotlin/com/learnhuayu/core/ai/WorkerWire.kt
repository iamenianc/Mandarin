package com.learnhuayu.core.ai

import com.learnhuayu.core.model.ScriptTurn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class InputAudioPart(
    val type: String,
    @SerialName("input_audio")
    val inputAudio: InputAudio,
)

@Serializable
internal data class InputAudio(
    val data: String,
    val format: String,
)

@Serializable
internal data class WorkerErrorBody(
    val error: String? = null,
    val status: Int? = null,
)

internal fun AudioClip.toInputAudioPart(): InputAudioPart = InputAudioPart(
    type = "input_audio",
    inputAudio = InputAudio(
        data = java.util.Base64.getEncoder().encodeToString(bytes),
        format = format.wireName,
    ),
)

internal fun SpeechSynthesisRequest.toBody(): SpeechSynthesisBody = when (this) {
    is SpeechSynthesisRequest.Pinyin -> SpeechSynthesisBody(pinyin = pinyin, voice = voice, language = language)
    is SpeechSynthesisRequest.ReplyText -> SpeechSynthesisBody(replyText = replyText, voice = voice, language = language)
}

internal fun ScriptTurn.toWire(): WireScriptTurn = WireScriptTurn(
    pinyin = pinyin,
    meaning = meaning,
    targetTones = targetTones,
)

internal fun WireScriptTurn.toScriptTurn(): ScriptTurn = ScriptTurn(
    pinyin = pinyin,
    meaning = meaning,
    targetTones = targetTones,
)

@Serializable
internal data class PronunciationFeedbackBody(
    val pinyin: String,
    val targetTones: List<Int>? = null,
    val learnerLevel: String? = null,
    val acousticEvidence: AcousticEvidence? = null,
    val audio: List<InputAudioPart>,
)

@Serializable
internal data class ConversationTurnBody(
    val scenario: String,
    val conversationState: ConversationState? = null,
    val targetDifficulty: String? = null,
    val audio: List<InputAudioPart>,
)

@Serializable
internal data class SpeechSynthesisBody(
    val pinyin: String? = null,
    val replyText: String? = null,
    val voice: String? = null,
    val language: String? = null,
)

@Serializable
internal data class ResponseTranscriptionBody(
    val audio: List<InputAudioPart>,
    val expectedOptions: List<String>? = null,
    val keywords: List<String>? = null,
)

@Serializable
internal data class ProgressSummaryBody(
    val attemptCounts: Map<String, Int>,
    val feedbackThemes: List<String>,
    val moduleIds: List<String>? = null,
    val lessonIds: List<String>? = null,
    val debriefThemes: List<String>? = null,
)

@Serializable
internal data class MandarinQaBody(
    val question: String? = null,
    val audio: List<InputAudioPart>? = null,
    val history: List<QaExchange>? = null,
    val learnerLevel: String? = null,
)

@Serializable
internal data class ExerciseGenerationBody(
    val moduleId: String,
    val itemType: ExerciseItemType,
    val theme: String? = null,
    val targetUnits: List<String>? = null,
    val difficulty: String? = null,
    val feedbackThemes: List<String>? = null,
    val count: Int? = null,
)

@Serializable
internal data class FieldMissionGenerationBody(
    val theme: String,
    val moduleContext: String? = null,
    val coveredContent: List<String>? = null,
    val learnerLevel: String? = null,
    val debriefThemes: List<String>? = null,
    val exchangeLength: Int? = null,
)

@Serializable
internal data class LocalTurnBody(
    val persona: MissionLocal,
    val mission: LocalTurnMissionBody,
    val audio: List<InputAudioPart>,
    val conversationState: ConversationState? = null,
    val targetDifficulty: String? = null,
)

@Serializable
internal data class LocalTurnMissionBody(
    val script: List<WireScriptTurn>,
    val goal: String? = null,
)

@Serializable
internal data class WireScriptTurn(
    val pinyin: String,
    val meaning: String,
    val targetTones: List<Int>,
)

@Serializable
internal data class GeneratedMissionBody(
    val script: List<WireScriptTurn>,
    val locals: List<MissionLocal>,
)
