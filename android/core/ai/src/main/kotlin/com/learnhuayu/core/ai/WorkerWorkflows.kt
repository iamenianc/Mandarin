package com.learnhuayu.core.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestTimeoutException
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException as ConcurrentCancellationException

class WorkerWorkflows internal constructor(
    private val client: HttpClient,
    private val config: WorkerHttpConfig,
    private val timeouts: WorkflowTimeouts = WorkflowTimeouts(),
    private val timeoutRunner: WorkflowTimeoutRunner = RealTimeoutRunner,
) : PronunciationFeedbackWorkflow,
    ConversationTurnWorkflow,
    SpeechSynthesisWorkflow,
    ResponseTranscriptionWorkflow,
    ProgressSummaryWorkflow,
    MandarinQaWorkflow,
    ExerciseGenerationWorkflow,
    FieldMissionGenerationWorkflow,
    LocalTurnWorkflow {

    override suspend fun evaluate(request: PronunciationFeedbackRequest): WorkflowResult<PronunciationFeedback> = postJson(
        WORKFLOW_PRONUNCIATION_FEEDBACK,
        PronunciationFeedback.serializer(),
        timeoutMillis = timeouts.pronunciationFeedbackMillis,
    ) {
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

    override suspend fun respond(request: ConversationTurnRequest): WorkflowResult<ConversationTurnReply> = postJson(
        WORKFLOW_CONVERSATION_TURN,
        ConversationTurnReply.serializer(),
        timeoutMillis = timeouts.conversationTurnMillis,
    ) {
        setBody(
            ConversationTurnBody(
                scenario = request.scenario,
                conversationState = request.conversationState,
                targetDifficulty = request.targetDifficulty,
                audio = listOf(request.audio.toInputAudioPart()),
            ),
        )
    }

    override suspend fun synthesize(request: SpeechSynthesisRequest): WorkflowResult<SynthesizedSpeech> = execute(
        WORKFLOW_SPEECH_SYNTHESIS,
        timeoutMillis = timeouts.speechSynthesisMillis,
        block = { setBody(request.toBody()) },
    ) { response ->
        val contentType = response.contentType()?.toString()
        if (contentType == null || !contentType.startsWith("audio/")) {
            WorkflowResult.Failure(
                WorkflowFailure.MalformedResponse("the worker returned a non-audio speech synthesis response"),
            )
        } else {
            WorkflowResult.Success(SynthesizedSpeech(bytes = response.readRawBytes(), contentType = contentType))
        }
    }

    override suspend fun transcribe(request: ResponseTranscriptionRequest): WorkflowResult<ResponseTranscription> = postJson(
        WORKFLOW_RESPONSE_TRANSCRIPTION,
        ResponseTranscription.serializer(),
        timeoutMillis = timeouts.responseTranscriptionMillis,
    ) {
        setBody(
            ResponseTranscriptionBody(
                audio = listOf(request.audio.toInputAudioPart()),
                expectedOptions = request.expectedOptions.takeIf { it.isNotEmpty() },
                keywords = request.keywords.takeIf { it.isNotEmpty() },
            ),
        )
    }

    override suspend fun summarize(request: ProgressSummaryRequest): WorkflowResult<ProgressSummary> = postJson(
        WORKFLOW_PROGRESS_SUMMARY,
        ProgressSummary.serializer(),
        timeoutMillis = timeouts.progressSummaryMillis,
    ) {
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

    override suspend fun ask(request: MandarinQaRequest): WorkflowResult<MandarinQaAnswer> = postJson(
        WORKFLOW_MANDARIN_QA,
        MandarinQaAnswer.serializer(),
        timeoutMillis = timeouts.mandarinQaMillis,
    ) {
        setBody(
            MandarinQaBody(
                question = request.question,
                audio = request.audio?.let { listOf(it.toInputAudioPart()) },
                history = request.history.takeIf { it.isNotEmpty() },
                learnerLevel = request.learnerLevel,
            ),
        )
    }

    override suspend fun generate(request: ExerciseGenerationRequest): WorkflowResult<GeneratedExercises> = postJson(
        WORKFLOW_EXERCISE_GENERATION,
        GeneratedExercises.serializer(),
        timeoutMillis = timeouts.exerciseGenerationMillis,
    ) {
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
        val result = postJson(
            WORKFLOW_FIELD_MISSION_GENERATION,
            GeneratedMissionBody.serializer(),
            timeoutMillis = timeouts.fieldMissionGenerationMillis,
        ) {
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

    override suspend fun respond(request: LocalTurnRequest): WorkflowResult<LocalTurnReply> = postJson(
        WORKFLOW_LOCAL_TURN,
        LocalTurnReply.serializer(),
        timeoutMillis = timeouts.localTurnMillis,
    ) {
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
        timeoutMillis: Long,
        block: HttpRequestBuilder.() -> Unit,
    ): WorkflowResult<T> = execute(slug, timeoutMillis, block) { response ->
        decode(response.bodyAsText(), deserializer)
    }

    private suspend fun <T> execute(
        slug: String,
        timeoutMillis: Long,
        block: HttpRequestBuilder.() -> Unit,
        onSuccess: suspend (HttpResponse) -> WorkflowResult<T>,
    ): WorkflowResult<T> {
        if (config.baseUrl.isBlank()) {
            return WorkflowResult.Failure(WorkflowFailure.BaseUrlMissing())
        }
        return try {
            val response = timeoutRunner.run(timeoutMillis) {
                client.post(endpoint(slug)) {
                    contentType(ContentType.Application.Json)
                    block()
                }
            }
            if (response.status.isSuccess()) {
                onSuccess(response)
            } else {
                WorkflowResult.Failure(failureFor(response.status.value, response.bodyAsText()))
            }
        } catch (cancellation: CancellationException) {
            if (cancellation is TimeoutCancellationException) {
                WorkflowResult.Failure(
                    WorkflowFailure.Timeout("the $slug workflow timed out after ${timeoutMillis}ms", timeoutMillis, cancellation),
                )
            } else {
                throw cancellation
            }
        } catch (concurrentCancellation: ConcurrentCancellationException) {
            // The Ktor engine can abort the call with its own cancellation type when the
            // budget above fires (observed as "Timed out waiting for N ms"). Map it to
            // Failure like any other timeout so caller fallback paths trigger. In the
            // rare case this stems from external cancellation rather than the budget,
            // the benign outcome is a fallback UI update during teardown.
            WorkflowResult.Failure(
                WorkflowFailure.Timeout(
                    "the $slug workflow timed out after ${timeoutMillis}ms",
                    timeoutMillis,
                    concurrentCancellation,
                ),
            )
        } catch (exception: Exception) {
            val timeoutCause = generateSequence<Throwable>(exception) { it.cause }.firstOrNull {
                it is HttpRequestTimeoutException || it is SocketTimeoutException
            }
            if (timeoutCause != null) {
                WorkflowResult.Failure(
                    WorkflowFailure.Timeout("the $slug workflow timed out", timeoutMillis, timeoutCause as Exception),
                )
            } else {
                WorkflowResult.Failure(WorkflowFailure.NetworkError(exception.message ?: "network error", exception))
            }
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

        fun create(baseUrl: String, timeouts: WorkflowTimeouts = WorkflowTimeouts()): WorkerWorkflows {
            val config = WorkerHttpConfig(baseUrl = baseUrl)
            return WorkerWorkflows(workerHttpClient(OkHttp.create(), config), config, timeouts)
        }
    }
}
