package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class ConversationTurnWorkflowTest {

    @Test
    fun `posts the spoken turn and state to the conversation turn endpoint`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(REPLY_JSON)
        }

        val result = workflows.respond(
            ConversationTurnRequest(
                scenario = "ordering tea at a teahouse",
                audio = wavClip("learner"),
                conversationState = ConversationState(
                    transcripts = listOf("ni3 hao3"),
                    corrections = listOf("watch the third tone"),
                ),
                targetDifficulty = "beginner",
            ),
        )

        val httpRequest = requireNotNull(captured)
        assertThat(httpRequest.method).isEqualTo(HttpMethod.Post)
        assertThat(httpRequest.url.toString()).isEqualTo("$TEST_BASE_URL/v1/wf/conversation-turn")
        val body = httpRequest.jsonBody().jsonObject
        assertThat(body["scenario"]!!.jsonPrimitive.content).isEqualTo("ordering tea at a teahouse")
        assertThat(body["targetDifficulty"]!!.jsonPrimitive.content).isEqualTo("beginner")
        assertThat(body["audio"]!!.jsonArray).hasSize(1)
        val state = body["conversationState"]!!.jsonObject
        assertThat(state["transcripts"]!!.jsonArray.single().jsonPrimitive.content).isEqualTo("ni3 hao3")
        assertThat(state["corrections"]!!.jsonArray.single().jsonPrimitive.content).isEqualTo("watch the third tone")
        assertThat(result.valueOrNull()!!.replyText).isEqualTo("nin2 xiang3 he1 shen2 me5?")
    }

    @Test
    fun `omits conversation state and difficulty when absent`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(REPLY_JSON)
        }

        workflows.respond(ConversationTurnRequest(scenario = "greetings", audio = wavClip("learner")))

        val body = requireNotNull(captured).jsonBody().jsonObject
        assertThat(body).doesNotContainKey("conversationState")
        assertThat(body).doesNotContainKey("targetDifficulty")
    }

    @Test
    fun `parses the gentle correction and next prompt`() = runTest {
        val workflows = testWorkflows { jsonResponse(REPLY_JSON) }

        val reply = workflows.respond(ConversationTurnRequest(scenario = "greetings", audio = wavClip("learner")))
            .valueOrNull()

        assertThat(reply).isNotNull()
        assertThat(reply!!.replyText).isEqualTo("nin2 xiang3 he1 shen2 me5?")
        assertThat(reply.gentleCorrection).isEqualTo("try a smoother third tone")
        assertThat(reply.nextPrompt).isEqualTo("ask for the price")
    }

    companion object {
        private const val REPLY_JSON =
            """{"replyText":"nin2 xiang3 he1 shen2 me5?","gentleCorrection":"try a smoother third tone","nextPrompt":"ask for the price"}"""
    }
}
