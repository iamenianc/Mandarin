package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertThrows
import org.junit.Test

class MandarinQaWorkflowTest {

    @Test
    fun `posts a text question with history to the mandarin qa endpoint`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(ANSWER_JSON)
        }

        val result = workflows.ask(
            MandarinQaRequest(
                question = "how do the four tones work?",
                history = listOf(
                    QaExchange(role = RaymondRole.USER, text = "ni3 hao3 means hello"),
                    QaExchange(role = RaymondRole.RAYMOND, text = "correct"),
                ),
                learnerLevel = "beginner",
            ),
        )

        val httpRequest = requireNotNull(captured)
        assertThat(httpRequest.method).isEqualTo(HttpMethod.Post)
        assertThat(httpRequest.url.toString()).isEqualTo("$TEST_BASE_URL/v1/wf/mandarin-qa")
        val body = httpRequest.jsonBody().jsonObject
        assertThat(body["question"]!!.jsonPrimitive.content).isEqualTo("how do the four tones work?")
        assertThat(body["learnerLevel"]!!.jsonPrimitive.content).isEqualTo("beginner")
        val history = body["history"]!!.jsonArray
        assertThat(history[0].jsonObject["role"]!!.jsonPrimitive.content).isEqualTo("user")
        assertThat(history[1].jsonObject["role"]!!.jsonPrimitive.content).isEqualTo("raymond")
        assertThat(history[0].jsonObject["text"]!!.jsonPrimitive.content).isEqualTo("ni3 hao3 means hello")
        assertThat(body).doesNotContainKey("audio")
        assertThat(result.valueOrNull()!!.answerText).isEqualTo("tones 1 to 4 rise, dip, and fall")
    }

    @Test
    fun `posts a spoken question when audio is supplied`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(ANSWER_JSON)
        }

        workflows.ask(MandarinQaRequest(audio = wavClip("spoken question")))

        val body = requireNotNull(captured).jsonBody().jsonObject
        assertThat(body["audio"]!!.jsonArray).hasSize(1)
        assertThat(body).doesNotContainKey("question")
    }

    @Test
    fun `parses the answer with examples and follow ups`() = runTest {
        val workflows = testWorkflows { jsonResponse(ANSWER_JSON) }

        val answer = workflows.ask(MandarinQaRequest(question = "how do the four tones work?")).valueOrNull()

        assertThat(answer).isNotNull()
        assertThat(answer!!.answerText).isEqualTo("tones 1 to 4 rise, dip, and fall")
        assertThat(answer.examples.single().pinyin).isEqualTo("ma1")
        assertThat(answer.examples.single().meaning).isEqualTo("mother")
        assertThat(answer.followUps).containsExactly("what is a tone pair?").inOrder()
    }

    @Test
    fun `rejects a request with neither question nor audio`() {
        assertThrows(IllegalArgumentException::class.java) {
            MandarinQaRequest()
        }
    }

    companion object {
        private const val ANSWER_JSON =
            """{"answerText":"tones 1 to 4 rise, dip, and fall","examples":[{"pinyin":"ma1","meaning":"mother"}],"followUps":["what is a tone pair?"]}"""
    }
}
