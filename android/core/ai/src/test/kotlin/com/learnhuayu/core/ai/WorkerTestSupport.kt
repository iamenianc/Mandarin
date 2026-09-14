package com.learnhuayu.core.ai

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal const val TEST_BASE_URL = "https://worker.test"

internal val testJson = Json { ignoreUnknownKeys = true }

internal fun testWorkflows(
    baseUrl: String = TEST_BASE_URL,
    timeouts: WorkflowTimeouts = WorkflowTimeouts(),
    timeoutRunner: WorkflowTimeoutRunner = NoTimeoutRunner,
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): WorkerWorkflows {
    val config = WorkerHttpConfig(baseUrl = baseUrl)
    return WorkerWorkflows(workerHttpClient(MockEngine(handler), config), config, timeouts, timeoutRunner)
}

internal fun timeoutWorkflows(
    baseUrl: String = TEST_BASE_URL,
    timeouts: WorkflowTimeouts = WorkflowTimeouts(),
    runner: WorkflowTimeoutRunner = RealTimeoutRunner,
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): WorkerWorkflows {
    val config = WorkerHttpConfig(baseUrl = baseUrl)
    return WorkerWorkflows(workerHttpClient(MockEngine(handler), config), config, timeouts, runner)
}

internal fun MockRequestHandleScope.jsonResponse(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData = respond(
    content = body,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
)

internal fun MockRequestHandleScope.audioResponse(
    bytes: ByteArray,
    contentType: String = "audio/wav",
): HttpResponseData = respond(
    content = bytes,
    status = HttpStatusCode.OK,
    headers = headersOf(HttpHeaders.ContentType, contentType),
)

internal fun HttpRequestData.bodyText(): String = when (val content = body) {
    is TextContent -> content.text
    is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
    else -> error("unexpected request body: $content")
}

internal fun HttpRequestData.jsonBody(): JsonElement = testJson.parseToJsonElement(bodyText())

internal fun <T> WorkflowResult<T>.failureOrNull(): WorkflowFailure? = (this as? WorkflowResult.Failure)?.failure

internal fun <T> WorkflowResult<T>.valueOrNull(): T? = (this as? WorkflowResult.Success)?.value

internal fun wavClip(content: String): AudioClip = AudioClip(content.toByteArray(), AudioFormat.WAV)
