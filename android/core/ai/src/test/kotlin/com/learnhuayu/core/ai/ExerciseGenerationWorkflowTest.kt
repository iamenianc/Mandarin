package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class ExerciseGenerationWorkflowTest {

    @Test
    fun `posts the generation request to the exercise generation endpoint`() = runTest {
        var captured: HttpRequestData? = null
        val workflows = testWorkflows { request ->
            captured = request
            jsonResponse(ITEMS_JSON)
        }

        val result = workflows.generate(
            ExerciseGenerationRequest(
                moduleId = "tones",
                itemType = ExerciseItemType.MINIMAL_PAIR,
                theme = "tone pairs",
                targetUnits = listOf("tones"),
                difficulty = "beginner",
                feedbackThemes = listOf("third tone"),
                count = 3,
            ),
        )

        val httpRequest = requireNotNull(captured)
        assertThat(httpRequest.method).isEqualTo(HttpMethod.Post)
        assertThat(httpRequest.url.toString()).isEqualTo("$TEST_BASE_URL/v1/wf/exercise-generation")
        val body = httpRequest.jsonBody().jsonObject
        assertThat(body["moduleId"]!!.jsonPrimitive.content).isEqualTo("tones")
        assertThat(body["itemType"]!!.jsonPrimitive.content).isEqualTo("minimalPair")
        assertThat(body["theme"]!!.jsonPrimitive.content).isEqualTo("tone pairs")
        assertThat(body["targetUnits"]!!.jsonArray.single().jsonPrimitive.content).isEqualTo("tones")
        assertThat(body["difficulty"]!!.jsonPrimitive.content).isEqualTo("beginner")
        assertThat(body["feedbackThemes"]!!.jsonArray.single().jsonPrimitive.content).isEqualTo("third tone")
        assertThat(body["count"]!!.jsonPrimitive.content).isEqualTo("3")
        assertThat(body).doesNotContainKey("audio")
        assertThat(result.valueOrNull()!!.items).hasSize(1)
    }

    @Test
    fun `parses generated items with optional hangul`() = runTest {
        val workflows = testWorkflows { jsonResponse(ITEMS_JSON) }

        val items = workflows.generate(
            ExerciseGenerationRequest(moduleId = "tones", itemType = ExerciseItemType.MINIMAL_PAIR),
        ).valueOrNull()!!.items

        val item = items.single()
        assertThat(item.type).isEqualTo(ExerciseItemType.MINIMAL_PAIR)
        assertThat(item.meaning).isEqualTo("mother")
        assertThat(item.pinyin).isEqualTo("ma1")
        assertThat(item.hangul).isEqualTo("마")
        assertThat(item.targetTones).containsExactly(1).inOrder()
        assertThat(item.distractors).containsExactly("ma3", "ma4").inOrder()
        assertThat(item.rationale).isEqualTo("contrasts tone 1 with tones 3 and 4")
    }

    companion object {
        private const val ITEMS_JSON =
            """{"items":[{"type":"minimalPair","meaning":"mother","pinyin":"ma1","hangul":"마","targetTones":[1],"distractors":["ma3","ma4"],"rationale":"contrasts tone 1 with tones 3 and 4"}]}"""
    }
}
