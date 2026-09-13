package com.learnhuayu.core.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException

class WorkerWorkflows internal constructor(
    private val client: HttpClient,
    private val config: WorkerHttpConfig,
) : PronunciationFeedbackWorkflow,
    ConversationTurnWorkflow,
    SpeechSynthesisWorkflow,
    ResponseTranscriptionWorkflow,
    ProgressSummaryWorkflow,
    MandarinQaWorkflow,
    ExerciseGenerationWorkflow,
    FieldMissionGenerationWorkflow,
    LocalTurnWorkflow {

    override suspend fun evaluate(request: PronunciationFeedbackRequest): WorkflowResult<PronunciationFeedback> = postJson(WORKFLOW_PRONUNCIATION_FEEDBACK, PronunciationFeedback.serializer()) {
        setBody(
            PronunciationFeedbackBody(
                pinyin = request.pinyin,
                targetTones = request.targetTones.takeIf { it.isNotEmpty() },
                learnerLevel = request.learnerLevel,
                acousticEvidence = request.acousticEvidence,
                audio = listOf(request.referenceAudio.toInputAudioPart(), request.attemptAudio.toInputAudioPart()),
            ),
        )
    }

    override suspend fun respond(request: ConversationTurnRequest): WorkflowResult<ConversationTurnReply> = postJson(WORKFLOW_CONVERSATION_TURN, ConversationTurnReply.serializer()) {
        setBody(
            ConversationTurnBody(
                scenario = request.scenario,
                conversationState = request.conversationState,
                targetDifficulty = request.targetDifficulty,
                audio = listOf(request.audio.toInputAudioPart()),
            ),
        )
    }

    override suspend fun synthesize(request: SpeechSynthesisRequest): WorkflowResult<SynthesizedSpeech> = execute(WORKFLOW_SPEECH_SYNTHESIS, { setBody(request.toBody()) }) { response ->
        val contentType = response.contentType()?.toString()
        if (contentType == null || !contentType.startsWith("audio/")) {
            WorkflowResult.Failure(
                WorkflowFailure.MalformedResponse("the worker returned a non-audio speech synthesis response"),
            )
        } else {
            WorkflowResult.Success(SynthesizedSpeech(bytes = response.readRawBytes(), contentType = contentType))
        }
    }

    override suspend fun transcribe(request: ResponseTranscriptionRequest): WorkflowResult<ResponseTranscription> = postJson(WORKFLOW_RESPONSE_TRANSCRIPTION, ResponseTranscription.serializer()) {
        setBody(
            ResponseTranscriptionBody(
                audio = listOf(request.audio.toInputAudioPart()),
                expectedOptions = request.expectedOptions.takeIf { it.isNotEmpty() },
                keywords = request.keywords.takeIf { it.isNotEmpty() },
            ),
        )
    }

    override suspend fun summarize(request: ProgressSummaryRequest): WorkflowResult<ProgressSummary> = postJson(WORKFLOW_PROGRESS_SUMMARY, ProgressSummary.serializer()) {
        setBody(
            ProgressSummaryBody(
                attemptCounts = request.attemptCounts,
                feedbackThemes = request.feedbackThemes,
                moduleIds = request.moduleIds.takeIf { it.isNotEmpty() },
                lessonIds = request.lessonIds.takeIf { it.isNotEmpty() },
                debriefThemes = request.debriefThemes.takeIf { it.isNotEmpty() },
            ),
        )
    }

    override suspend fun ask(request: MandarinQaRequest): WorkflowResult<MandarinQaAnswer> = postJson(WORKFLOW_MANDARIN_QA, MandarinQaAnswer.serializer()) {
        setBody(
            MandarinQaBody(
                question = request.question,
                audio = request.audio?.let { listOf(it.toInputAudioPart()) },
                history = request.history.takeIf { it.isNotEmpty() },
                learnerLevel = request.learnerLevel,
            ),
        )
    }

    override suspend fun generate(request: ExerciseGenerationRequest): WorkflowResult<GeneratedExercises> = postJson(WORKFLOW_EXERCISE_GENERATION, GeneratedExercises.serializer()) {
        setBody(
            ExerciseGenerationBody(
                moduleId = request.moduleId,
                itemType = request.itemType,
                theme = request.theme,
                targetUnits = request.targetUnits.takeIf { it.isNotEmpty() },
                difficulty = request.difficulty,
                feedbackThemes = request.feedbackThemes.takeIf { it.isNotEmpty() },
                count = request.count,
            ),
        )
    }

    override suspend fun generate(request: FieldMissionGenerationRequest): WorkflowResult<GeneratedMission> = when (
        val result = postJson(WORKFLOW_FIELD_MISSION_GENERATION, GeneratedMissionBody.serializer()) {
            setBody(
                FieldMissionGenerationBody(
                    theme = request.theme,
                    moduleContext = request.moduleContext,
                    coveredContent = request.coveredContent.takeIf { it.isNotEmpty() },
                    learnerLevel = request.learnerLevel,
                    debriefThemes = request.debriefThemes.takeIf { it.isNotEmpty() },
                    exchangeLength = request.exchangeLength,
                ),
            )
        }
    ) {
        is WorkflowResult.Success -> WorkflowResult.Success(
            GeneratedMission(
                script = result.value.script.map { it.toScriptTurn() },
                locals = result.value.locals,
            ),
        )
        is WorkflowResult.Failure -> result
    }

    override suspend fun respond(request: LocalTurnRequest): WorkflowResult<LocalTurnReply> = postJson(WORKFLOW_LOCAL_TURN, LocalTurnReply.serializer()) {
        setBody(
            LocalTurnBody(
                persona = request.persona,
                mission = LocalTurnMissionBody(
                    script = request.mission.script.map { it.toWire() },
                    goal = request.mission.goal,
                ),
                audio = listOf(request.audio.toInputAudioPart()),
                conversationState = request.conversationState,
                targetDifficulty = request.targetDifficulty,
            ),
        )
    }

    private suspend fun <T> postJson(
        slug: String,
        deserializer: DeserializationStrategy<T>,
        block: HttpRequestBuilder.() -> Unit,
    ): WorkflowResult<T> = execute(slug, block) { response ->
        decode(response.bodyAsText(), deserializer)
    }

    private suspend fun <T> execute(
        slug: String,
        block: HttpRequestBuilder.() -> Unit,
        onSuccess: suspend (HttpResponse) -> WorkflowResult<T>,
    ): WorkflowResult<T> {
        if (config.baseUrl.isBlank()) {
            return WorkflowResult.Failure(WorkflowFailure.BaseUrlMissing())
        }
        return try {
            val response = client.post(endpoint(slug)) {
                contentType(ContentType.Application.Json)
                block()
            }
            if (response.status.isSuccess()) {
                onSuccess(response)
            } else {
                WorkflowResult.Failure(failureFor(response.status.value, response.bodyAsText()))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            WorkflowResult.Failure(WorkflowFailure.NetworkError(exception.message ?: "network error", exception))
        }
    }

    private fun <T> decode(text: String, deserializer: DeserializationStrategy<T>): WorkflowResult<T> = try {
        WorkflowResult.Success(WorkerJson.decodeFromString(deserializer, text))
    } catch (exception: SerializationException) {
        WorkflowResult.Failure(WorkflowFailure.MalformedResponse(exception.message ?: "malformed worker response"))
    }

    private fun failureFor(status: Int, body: String): WorkflowFailure {
        val error = runCatching {
            WorkerJson.decodeFromString(WorkerErrorBody.serializer(), body).error
        }.getOrNull()
        val message = error?.takeIf { it.isNotBlank() } ?: "worker returned HTTP $status"
        return when (status) {
            400 -> WorkflowFailure.InvalidRequest(message)
            404 -> WorkflowFailure.RouteNotFound(message)
            413 -> WorkflowFailure.PayloadTooLarge(message)
            501 -> WorkflowFailure.ProviderNotConfigured(message)
            502 -> when (message) {
                "upstream error" -> WorkflowFailure.UpstreamError(message)
                "invalid upstream response" -> WorkflowFailure.InvalidUpstreamResponse(message)
                else -> WorkflowFailure.UnexpectedStatus(status, message)
            }
            else -> WorkflowFailure.UnexpectedStatus(status, message)
        }
    }

    private fun endpoint(slug: String): String = "${config.baseUrl.trimEnd('/')}/v1/wf/$slug"

    companion object {
        private const val WORKFLOW_PRONUNCIATION_FEEDBACK = "pronunciation-feedback"
        private const val WORKFLOW_CONVERSATION_TURN = "conversation-turn"
        private const val WORKFLOW_SPEECH_SYNTHESIS = "speech-synthesis"
        private const val WORKFLOW_RESPONSE_TRANSCRIPTION = "response-transcription"
        private const val WORKFLOW_PROGRESS_SUMMARY = "progress-summary"
        private const val WORKFLOW_MANDARIN_QA = "mandarin-qa"
        private const val WORKFLOW_EXERCISE_GENERATION = "exercise-generation"
        private const val WORKFLOW_FIELD_MISSION_GENERATION = "field-mission-generation"
        private const val WORKFLOW_LOCAL_TURN = "local-turn"

        fun create(baseUrl: String): WorkerWorkflows {
            val config = WorkerHttpConfig(baseUrl = baseUrl)
            return WorkerWorkflows(workerHttpClient(OkHttp.create(), config), config)
        }
    }
}
