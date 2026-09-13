package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class ProgressSummaryWorkflowTest {

    @Test
    fun `posts aggregated metadata to the progress summary endpoint`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(SUMMARY_JSON)
        }

        val result = workflows.summarize(
            ProgressSummaryRequest(
                attemptCounts = mapOf("phrase-ni3-hao3" to 4, "tone-3" to 2),
                feedbackThemes = listOf("third tone", "final -ao"),
                moduleIds = listOf("tones"),
                lessonIds = listOf("tones-1"),
                debriefThemes = listOf("tone breakdown"),
            ),
        )

        val httpRequest = requireNotNull(captured)
        assertThat(httpRequest.method).isEqualTo(HttpMethod.Post)
        assertThat(httpRequest.url.toString()).isEqualTo("$TEST_BASE_URL/v1/wf/progress-summary")
        val body = httpRequest.jsonBody().jsonObject
        assertThat(body["attemptCounts"]!!.jsonObject["phrase-ni3-hao3"]!!.jsonPrimitive.int).isEqualTo(4)
        assertThat(body["attemptCounts"]!!.jsonObject["tone-3"]!!.jsonPrimitive.int).isEqualTo(2)
        assertThat(body["feedbackThemes"]!!.jsonArray.map { it.jsonPrimitive.content })
            .containsExactly("third tone", "final -ao")
            .inOrder()
        assertThat(body["moduleIds"]!!.jsonArray.single().jsonPrimitive.content).isEqualTo("tones")
        assertThat(body["lessonIds"]!!.jsonArray.single().jsonPrimitive.content).isEqualTo("tones-1")
        assertThat(body["debriefThemes"]!!.jsonArray.single().jsonPrimitive.content).isEqualTo("tone breakdown")
        assertThat(body).doesNotContainKey("audio")
        assertThat(result.valueOrNull()!!.summaryText).isEqualTo("four attempts on the greeting this week")
    }

    @Test
    fun `parses the summary and focus areas`() = runTest {
        val workflows = testWorkflows { jsonResponse(SUMMARY_JSON) }

        val summary = workflows.summarize(
            ProgressSummaryRequest(
                attemptCounts = mapOf("phrase-ni3-hao3" to 4),
                feedbackThemes = listOf("third tone"),
            ),
        ).valueOrNull()

        assertThat(summary).isNotNull()
        assertThat(summary!!.summaryText).isEqualTo("four attempts on the greeting this week")
        assertThat(summary.focusAreas).containsExactly("third tone", "rhythm").inOrder()
    }

    companion object {
        private const val SUMMARY_JSON =
            """{"summaryText":"four attempts on the greeting this week","focusAreas":["third tone","rhythm"]}"""
    }
}
