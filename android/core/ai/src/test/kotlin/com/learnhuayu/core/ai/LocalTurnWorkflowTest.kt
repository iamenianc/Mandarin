package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.model.ScriptTurn
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class LocalTurnWorkflowTest {

    @Test
    fun `posts persona, mission, and audio to the local turn endpoint`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(REPLY_JSON)
        }

        val result = workflows.respond(
            LocalTurnRequest(
                persona = local(),
                mission = LocalTurnMission(
                    script = listOf(ScriptTurn(pinyin = "ni3 hao3", meaning = "hello", targetTones = listOf(3, 3))),
                    goal = "greet the teahouse keeper",
                ),
                audio = wavClip("learner"),
                conversationState = ConversationState(transcripts = listOf("ni3 hao3")),
                targetDifficulty = "beginner",
            ),
        )

        val httpRequest = requireNotNull(captured)
        assertThat(httpRequest.method).isEqualTo(HttpMethod.Post)
        assertThat(httpRequest.url.toString()).isEqualTo("$TEST_BASE_URL/v1/wf/local-turn")
        val body = httpRequest.jsonBody().jsonObject
        val persona = body["persona"]!!.jsonObject
        assertThat(persona["label"]!!.jsonPrimitive.content).isEqualTo("the teahouse keeper")
        assertThat(persona["settingRole"]!!.jsonPrimitive.content).isEqualTo("server")
        assertThat(persona["personality"]!!.jsonPrimitive.content).isEqualTo("warm and chatty")
        assertThat(persona["voiceProfile"]!!.jsonPrimitive.content).isEqualTo("low and unhurried")
        assertThat(persona["pace"]!!.jsonPrimitive.content).isEqualTo("slow")
        val mission = body["mission"]!!.jsonObject
        assertThat(mission["goal"]!!.jsonPrimitive.content).isEqualTo("greet the teahouse keeper")
        assertThat(mission["script"]!!.jsonArray.single().jsonObject["pinyin"]!!.jsonPrimitive.content).isEqualTo("ni3 hao3")
        assertThat(body["audio"]!!.jsonArray).hasSize(1)
        assertThat(body["targetDifficulty"]!!.jsonPrimitive.content).isEqualTo("beginner")
        assertThat(result.valueOrNull()!!.replyText).isEqualTo("ni3 hao3! jin4 lai2 ba5.")
    }

    @Test
    fun `parses the in-character reply`() = runTest {
        val workflows = testWorkflows { jsonResponse(REPLY_JSON) }

        val reply = workflows.respond(
            LocalTurnRequest(
                persona = local(),
                mission = LocalTurnMission(
                    script = listOf(ScriptTurn(pinyin = "ni3 hao3", meaning = "hello", targetTones = listOf(3, 3))),
                ),
                audio = wavClip("learner"),
            ),
        ).valueOrNull()

        assertThat(reply).isNotNull()
        assertThat(reply!!.replyText).isEqualTo("ni3 hao3! jin4 lai2 ba5.")
        assertThat(reply.understandingSignal).isEqualTo("smiles and waves")
        assertThat(reply.nextLocalPrompt).isEqualTo("ask what the learner wants")
    }

    private fun local() = MissionLocal(
        label = "the teahouse keeper",
        settingRole = "server",
        personality = "warm and chatty",
        voiceProfile = "low and unhurried",
        pace = "slow",
    )

    companion object {
        private const val REPLY_JSON =
            """{"replyText":"ni3 hao3! jin4 lai2 ba5.","understandingSignal":"smiles and waves","nextLocalPrompt":"ask what the learner wants"}"""
    }
}
