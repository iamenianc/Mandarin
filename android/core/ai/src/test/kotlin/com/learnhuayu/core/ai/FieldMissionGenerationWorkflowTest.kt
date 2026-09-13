package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class FieldMissionGenerationWorkflowTest {

    @Test
    fun `posts the mission request to the field mission generation endpoint`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(MISSION_JSON)
        }

        val result = workflows.generate(
            FieldMissionGenerationRequest(
                theme = "greetings",
                moduleContext = "vocabulary",
                coveredContent = listOf("ni3 hao3"),
                learnerLevel = "beginner",
                debriefThemes = listOf("third tone"),
                exchangeLength = 2,
            ),
        )

        val httpRequest = requireNotNull(captured)
        assertThat(httpRequest.method).isEqualTo(HttpMethod.Post)
        assertThat(httpRequest.url.toString()).isEqualTo("$TEST_BASE_URL/v1/wf/field-mission-generation")
        val body = httpRequest.jsonBody().jsonObject
        assertThat(body["theme"]!!.jsonPrimitive.content).isEqualTo("greetings")
        assertThat(body["moduleContext"]!!.jsonPrimitive.content).isEqualTo("vocabulary")
        assertThat(body["coveredContent"]!!.jsonArray.single().jsonPrimitive.content).isEqualTo("ni3 hao3")
        assertThat(body["learnerLevel"]!!.jsonPrimitive.content).isEqualTo("beginner")
        assertThat(body["debriefThemes"]!!.jsonArray.single().jsonPrimitive.content).isEqualTo("third tone")
        assertThat(body["exchangeLength"]!!.jsonPrimitive.content).isEqualTo("2")
        assertThat(body).doesNotContainKey("audio")
        assertThat(result.valueOrNull()!!.script).hasSize(2)
    }

    @Test
    fun `parses the script turns and the five locals`() = runTest {
        val workflows = testWorkflows { jsonResponse(MISSION_JSON) }

        val mission = workflows.generate(FieldMissionGenerationRequest(theme = "greetings")).valueOrNull()

        assertThat(mission).isNotNull()
        assertThat(mission!!.script).hasSize(2)
        assertThat(mission.script[0].pinyin).isEqualTo("ni3 hao3")
        assertThat(mission.script[0].meaning).isEqualTo("hello")
        assertThat(mission.script[0].targetTones).containsExactly(3, 3).inOrder()
        assertThat(mission.script[1].pinyin).isEqualTo("ni3 hao3 ma5")
        assertThat(mission.locals).hasSize(5)
        assertThat(mission.locals[0].label).isEqualTo("the teahouse keeper")
        assertThat(mission.locals[0].settingRole).isEqualTo("server")
        assertThat(mission.locals[0].personality).isEqualTo("warm and chatty")
        assertThat(mission.locals[0].voiceProfile).isEqualTo("low and unhurried")
        assertThat(mission.locals[0].pace).isEqualTo("slow")
        assertThat(mission.locals[4].label).isEqualTo("the neighbor")
    }

    companion object {
        private val LOCALS =
            (1..5).joinToString(separator = ",") { index ->
                """{"label":"${if (index == 1) {
                    "the teahouse keeper"
                } else if (index == 5) {
                    "the neighbor"
                } else {
                    "local $index"
                }}","settingRole":"server","personality":"warm and chatty","voiceProfile":"low and unhurried","pace":"slow"}"""
            }

        private val MISSION_JSON =
            """{"script":[{"pinyin":"ni3 hao3","meaning":"hello","targetTones":[3,3]},{"pinyin":"ni3 hao3 ma5","meaning":"how are you?","targetTones":[3,3,5]}],"locals":[$LOCALS]}"""
    }
}
