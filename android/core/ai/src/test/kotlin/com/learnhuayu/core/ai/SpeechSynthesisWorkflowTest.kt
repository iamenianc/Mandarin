package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class SpeechSynthesisWorkflowTest {

    @Test
    fun `posts pinyin to the speech synthesis endpoint`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            audioResponse(AUDIO_BYTES)
        }

        val result = workflows.synthesize(SpeechSynthesisRequest.Pinyin(pinyin = "ni3 hao3", voice = "kokoro"))

        val httpRequest = requireNotNull(captured)
        assertThat(httpRequest.method).isEqualTo(HttpMethod.Post)
        assertThat(httpRequest.url.toString()).isEqualTo("$TEST_BASE_URL/v1/wf/speech-synthesis")
        val body = httpRequest.jsonBody().jsonObject
        assertThat(body["pinyin"]!!.jsonPrimitive.content).isEqualTo("ni3 hao3")
        assertThat(body["voice"]!!.jsonPrimitive.content).isEqualTo("kokoro")
        assertThat(body).doesNotContainKey("replyText")
        val speech = result.valueOrNull()
        assertThat(speech).isNotNull()
        assertThat(speech!!.bytes).isEqualTo(AUDIO_BYTES)
        assertThat(speech.contentType).isEqualTo("audio/wav")
    }

    @Test
    fun `posts reply text for a conversation reply`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            audioResponse(AUDIO_BYTES, contentType = "audio/mpeg")
        }

        workflows.synthesize(SpeechSynthesisRequest.ReplyText(replyText = "nin2 hao3", language = "cmn"))

        val body = requireNotNull(captured).jsonBody().jsonObject
        assertThat(body["replyText"]!!.jsonPrimitive.content).isEqualTo("nin2 hao3")
        assertThat(body["language"]!!.jsonPrimitive.content).isEqualTo("cmn")
        assertThat(body).doesNotContainKey("pinyin")
    }

    @Test
    fun `maps the unconfigured provider status to ProviderNotConfigured`() = runTest {
        val workflows = testWorkflows {
            jsonResponse(
                """{"error":"speech synthesis provider not configured","status":501}""",
                HttpStatusCode.NotImplemented,
            )
        }

        val failure = workflows.synthesize(SpeechSynthesisRequest.Pinyin(pinyin = "ni3 hao3")).failureOrNull()

        assertThat(failure).isEqualTo(
            WorkflowFailure.ProviderNotConfigured("speech synthesis provider not configured"),
        )
    }

    @Test
    fun `rejects a success response that is not audio`() = runTest {
        val workflows = testWorkflows { jsonResponse("""{"bytes":"nope"}""") }

        val failure = workflows.synthesize(SpeechSynthesisRequest.Pinyin(pinyin = "ni3 hao3")).failureOrNull()

        assertThat(failure).isInstanceOf(WorkflowFailure.MalformedResponse::class.java)
    }

    companion object {
        private val AUDIO_BYTES = byteArrayOf(1, 2, 3, 4)
    }
}
