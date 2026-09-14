package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class WorkerFailureMappingTest {

    @Test
    fun `maps 400 to InvalidRequest`() = runTest {
        val failure = evaluate {
            jsonResponse(
                """{"error":"pinyin must not contain Chinese characters","status":400}""",
                HttpStatusCode.BadRequest,
            )
        }

        assertThat(failure).isEqualTo(WorkflowFailure.InvalidRequest("pinyin must not contain Chinese characters"))
    }

    @Test
    fun `maps 404 to RouteNotFound`() = runTest {
        val failure = evaluate {
            jsonResponse("""{"error":"not found","status":404}""", HttpStatusCode.NotFound)
        }

        assertThat(failure).isEqualTo(WorkflowFailure.RouteNotFound("not found"))
    }

    @Test
    fun `maps a body cap rejection to PayloadTooLarge`() = runTest {
        val failure = evaluate {
            jsonResponse("""{"error":"payload too large","status":413}""", HttpStatusCode.fromValue(413))
        }

        assertThat(failure).isEqualTo(WorkflowFailure.PayloadTooLarge("payload too large"))
    }

    @Test
    fun `maps an audio part cap rejection to PayloadTooLarge`() = runTest {
        val failure = evaluate {
            jsonResponse("""{"error":"audio part too large","status":413}""", HttpStatusCode.fromValue(413))
        }

        assertThat(failure).isEqualTo(WorkflowFailure.PayloadTooLarge("audio part too large"))
    }

    @Test
    fun `maps 502 upstream errors to UpstreamError`() = runTest {
        val failure = evaluate {
            jsonResponse("""{"error":"upstream error","status":502}""", HttpStatusCode.BadGateway)
        }

        assertThat(failure).isEqualTo(WorkflowFailure.UpstreamError("upstream error"))
    }

    @Test
    fun `maps 502 schema failures to InvalidUpstreamResponse`() = runTest {
        val failure = evaluate {
            jsonResponse("""{"error":"invalid upstream response","status":502}""", HttpStatusCode.BadGateway)
        }

        assertThat(failure).isEqualTo(WorkflowFailure.InvalidUpstreamResponse("invalid upstream response"))
    }

    @Test
    fun `maps unknown statuses to UnexpectedStatus`() = runTest {
        val failure = evaluate {
            jsonResponse("""{"error":"boom","status":500}""", HttpStatusCode.InternalServerError)
        }

        assertThat(failure).isEqualTo(WorkflowFailure.UnexpectedStatus(500, "boom"))
    }

    @Test
    fun `maps an unparseable success body to MalformedResponse`() = runTest {
        val failure = evaluate { jsonResponse("not json") }

        assertThat(failure).isInstanceOf(WorkflowFailure.MalformedResponse::class.java)
    }

    @Test
    fun `maps a schema mismatched success body to MalformedResponse`() = runTest {
        val failure = evaluate { jsonResponse("""{"weakestUnit":"hao3"}""") }

        assertThat(failure).isInstanceOf(WorkflowFailure.MalformedResponse::class.java)
    }

    @Test
    fun `maps transport failures to NetworkError`() = runTest {
        val failure = evaluate { throw IOException("offline") }

        assertThat(failure).isInstanceOf(WorkflowFailure.NetworkError::class.java)
    }

    @Test
    fun `maps engine timeouts to Timeout`() = runTest {
        val failure = timeoutWorkflows { throw HttpRequestTimeoutException("timeout", 1_000L, null) }
            .evaluate(feedbackRequest()).failureOrNull()

        assertThat(failure).isInstanceOf(WorkflowFailure.Timeout::class.java)
    }

    @Test
    fun `fails closed when the base url is missing`() = runTest {
        var called = false
        val workflows = testWorkflows(baseUrl = "") {
            called = true
            jsonResponse("{}")
        }

        val failure = workflows.evaluate(feedbackRequest()).failureOrNull()

        assertThat(failure).isEqualTo(WorkflowFailure.BaseUrlMissing())
        assertThat(called).isFalse()
    }

    private suspend fun evaluate(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): WorkflowFailure? = testWorkflows(handler = handler).evaluate(feedbackRequest()).failureOrNull()

    private fun feedbackRequest() = PronunciationFeedbackRequest(
        pinyin = "ni3 hao3",
        referenceAudio = wavClip("reference"),
        attemptAudio = wavClip("attempt"),
    )
}
