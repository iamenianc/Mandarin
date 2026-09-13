package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class ResponseTranscriptionWorkflowTest {

    @Test
    fun `posts audio with expected options to the transcription endpoint`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(TRANSCRIPT_JSON)
        }

        val result = workflows.transcribe(
            ResponseTranscriptionRequest(
                audio = wavClip("answer"),
                expectedOptions = listOf("option-a", "option-b"),
                keywords = listOf("tea"),
            ),
        )

        val httpRequest = requireNotNull(captured)
        assertThat(httpRequest.method).isEqualTo(HttpMethod.Post)
        assertThat(httpRequest.url.toString()).isEqualTo("$TEST_BASE_URL/v1/wf/response-transcription")
        val body = httpRequest.jsonBody().jsonObject
        assertThat(body["audio"]!!.jsonArray).hasSize(1)
        assertThat(body["expectedOptions"]!!.jsonArray.map { it.jsonPrimitive.content })
            .containsExactly("option-a", "option-b")
            .inOrder()
        assertThat(body["keywords"]!!.jsonArray.single().jsonPrimitive.content).isEqualTo("tea")
        assertThat(result.valueOrNull()!!.transcript).isEqualTo("wo3 yao4 yi1 bei1 cha2")
    }

    @Test
    fun `omits expected options and keywords when empty`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(TRANSCRIPT_JSON)
        }

        workflows.transcribe(ResponseTranscriptionRequest(audio = wavClip("answer")))

        val body = requireNotNull(captured).jsonBody().jsonObject
        assertThat(body).doesNotContainKey("expectedOptions")
        assertThat(body).doesNotContainKey("keywords")
    }

    @Test
    fun `parses transcript, matched option, and confidence`() = runTest {
        val workflows = testWorkflows { jsonResponse(TRANSCRIPT_JSON) }

        val transcription = workflows.transcribe(ResponseTranscriptionRequest(audio = wavClip("answer")))
            .valueOrNull()

        assertThat(transcription).isNotNull()
        assertThat(transcription!!.transcript).isEqualTo("wo3 yao4 yi1 bei1 cha2")
        assertThat(transcription.matchedOptionId).isEqualTo("option-a")
        assertThat(transcription.confidence).isEqualTo(0.91)
    }

    companion object {
        private const val TRANSCRIPT_JSON =
            """{"transcript":"wo3 yao4 yi1 bei1 cha2","matchedOptionId":"option-a","confidence":0.91}"""
    }
}
