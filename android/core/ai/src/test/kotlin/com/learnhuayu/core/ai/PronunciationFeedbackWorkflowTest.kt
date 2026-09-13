package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.util.Base64

class PronunciationFeedbackWorkflowTest {

    @Test
    fun `posts reference and attempt audio to the pronunciation feedback endpoint`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(COACHING_JSON)
        }

        val result = workflows.evaluate(request())

        val httpRequest = requireNotNull(captured)
        assertThat(httpRequest.method).isEqualTo(HttpMethod.Post)
        assertThat(httpRequest.url.toString()).isEqualTo("$TEST_BASE_URL/v1/wf/pronunciation-feedback")
        val body = httpRequest.jsonBody().jsonObject
        assertThat(body["pinyin"]!!.jsonPrimitive.content).isEqualTo("ni3 hao3")
        assertThat(body).doesNotContainKey("acousticEvidence")
        val audio = body["audio"]!!.jsonArray
        assertThat(audio).hasSize(2)
        val reference = audio[0].jsonObject
        val attempt = audio[1].jsonObject
        assertThat(reference["type"]!!.jsonPrimitive.content).isEqualTo("input_audio")
        assertThat(reference["input_audio"]!!.jsonObject["format"]!!.jsonPrimitive.content).isEqualTo("wav")
        assertThat(reference["input_audio"]!!.jsonObject["data"]!!.jsonPrimitive.content)
            .isEqualTo(Base64.getEncoder().encodeToString("reference".toByteArray()))
        assertThat(attempt["input_audio"]!!.jsonObject["data"]!!.jsonPrimitive.content)
            .isEqualTo(Base64.getEncoder().encodeToString("attempt".toByteArray()))
        assertThat(result.valueOrNull()!!.weakestUnit).isEqualTo("hao3")
    }

    @Test
    fun `serializes measured evidence with the worker field names`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(COACHING_JSON)
        }

        workflows.evaluate(
            request(
                targetTones = listOf(3, 3),
                learnerLevel = "beginner",
                acousticEvidence = AcousticEvidence(
                    phrase = "ni3 hao3",
                    syllables = listOf(
                        AcousticSyllable(
                            pinyin = "ni3",
                            expectedTone = 3,
                            observed = "dip",
                            onsetLevel = 4,
                            offsetLevel = 3,
                            voicingRatio = 0.8,
                            voicedMs = 210,
                            loudness = "medium",
                            note = "short dip",
                        ),
                    ),
                ),
            ),
        )

        val body = requireNotNull(captured).jsonBody().jsonObject
        assertThat(body["targetTones"]!!.jsonArray.map { it.jsonPrimitive.int }).containsExactly(3, 3).inOrder()
        assertThat(body["learnerLevel"]!!.jsonPrimitive.content).isEqualTo("beginner")
        val evidence = body["acousticEvidence"]!!.jsonObject
        assertThat(evidence["phrase"]!!.jsonPrimitive.content).isEqualTo("ni3 hao3")
        val syllable = evidence["syllables"]!!.jsonArray.single().jsonObject
        assertThat(syllable["pinyin"]!!.jsonPrimitive.content).isEqualTo("ni3")
        assertThat(syllable["expectedTone"]!!.jsonPrimitive.int).isEqualTo(3)
        assertThat(syllable["observed"]!!.jsonPrimitive.content).isEqualTo("dip")
        assertThat(syllable["onsetLevel"]!!.jsonPrimitive.int).isEqualTo(4)
        assertThat(syllable["offsetLevel"]!!.jsonPrimitive.int).isEqualTo(3)
        assertThat(syllable["voicingRatio"]!!.jsonPrimitive.double).isEqualTo(0.8)
        assertThat(syllable["voicedMs"]!!.jsonPrimitive.int).isEqualTo(210)
        assertThat(syllable["loudness"]!!.jsonPrimitive.content).isEqualTo("medium")
        assertThat(syllable["note"]!!.jsonPrimitive.content).isEqualTo("short dip")
    }

    @Test
    fun `parses the coaching response`() = runTest {
        val workflows = testWorkflows { jsonResponse(COACHING_JSON) }

        val feedback = workflows.evaluate(request()).valueOrNull()

        assertThat(feedback).isNotNull()
        assertThat(feedback!!.weakestUnit).isEqualTo("hao3")
        assertThat(feedback.issue).isEqualTo("the dip is too shallow")
        assertThat(feedback.tip).isEqualTo("start lower and dip deeper")
        assertThat(feedback.encouragement).isEqualTo("good first try")
        assertThat(feedback.replayHint).isEqualTo("listen for the dip")
    }

    @Test
    fun `maps a rejected request to InvalidRequest`() = runTest {
        val workflows = testWorkflows {
            jsonResponse(
                """{"error":"pinyin must be a non-empty string","status":400}""",
                HttpStatusCode.BadRequest,
            )
        }

        val failure = workflows.evaluate(request()).failureOrNull()

        assertThat(failure).isEqualTo(WorkflowFailure.InvalidRequest("pinyin must be a non-empty string"))
    }

    private fun request(
        targetTones: List<Int> = emptyList(),
        learnerLevel: String? = null,
        acousticEvidence: AcousticEvidence? = null,
    ) = PronunciationFeedbackRequest(
        pinyin = "ni3 hao3",
        referenceAudio = wavClip("reference"),
        attemptAudio = wavClip("attempt"),
        targetTones = targetTones,
        learnerLevel = learnerLevel,
        acousticEvidence = acousticEvidence,
    )

    companion object {
        private const val COACHING_JSON =
            """{"weakestUnit":"hao3","issue":"the dip is too shallow","tip":"start lower and dip deeper","encouragement":"good first try","replayHint":"listen for the dip"}"""
    }
}
