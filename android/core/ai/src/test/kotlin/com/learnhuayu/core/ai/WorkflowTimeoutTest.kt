package com.learnhuayu.core.ai

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.model.ScriptTurn
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test

class WorkflowTimeoutTest {

    @Test
    fun `WF-1 timeout maps to Failure instead of throwing`() = runTest {
        val runner = FakeTimeoutRunner()
        val workflows = timeoutWorkflows(
            timeouts = WorkflowTimeouts(pronunciationFeedbackMillis = 10L),
            runner = runner,
        ) { jsonResponse(COACHING_JSON) }

        val result = workflows.evaluate(
            PronunciationFeedbackRequest(
                pinyin = "ni3 hao3",
                referenceAudio = wavClip("reference"),
                attemptAudio = wavClip("attempt"),
            ),
        )

        val failure = result.failureOrNull()
        assertThat(failure).isInstanceOf(WorkflowFailure.Timeout::class.java)
        assertThat((failure as WorkflowFailure.Timeout).timeoutMillis).isEqualTo(10L)
        assertThat(runner.seenMillis).containsExactly(10L)
    }

    @Test
    fun `WF-4 timeout maps to Failure instead of throwing`() = runTest {
        val runner = FakeTimeoutRunner()
        val workflows = timeoutWorkflows(
            timeouts = WorkflowTimeouts(responseTranscriptionMillis = 11L),
            runner = runner,
        ) { jsonResponse(TRANSCRIPT_JSON) }

        val result = workflows.transcribe(ResponseTranscriptionRequest(audio = wavClip("answer")))

        val failure = result.failureOrNull()
        assertThat(failure).isInstanceOf(WorkflowFailure.Timeout::class.java)
        assertThat((failure as WorkflowFailure.Timeout).timeoutMillis).isEqualTo(11L)
        assertThat(runner.seenMillis).containsExactly(11L)
    }

    @Test
    fun `WF-2 timeout maps to Failure instead of throwing`() = runTest {
        val runner = FakeTimeoutRunner()
        val workflows = timeoutWorkflows(
            timeouts = WorkflowTimeouts(conversationTurnMillis = 12L),
            runner = runner,
        ) { jsonResponse(CONVERSATION_JSON) }

        val result = workflows.respond(
            ConversationTurnRequest(scenario = "greetings", audio = wavClip("learner")),
        )

        val failure = result.failureOrNull()
        assertThat(failure).isInstanceOf(WorkflowFailure.Timeout::class.java)
        assertThat((failure as WorkflowFailure.Timeout).timeoutMillis).isEqualTo(12L)
        assertThat(runner.seenMillis).containsExactly(12L)
    }

    @Test
    fun `WF-10 timeout maps to Failure instead of throwing`() = runTest {
        val runner = FakeTimeoutRunner()
        val workflows = timeoutWorkflows(
            timeouts = WorkflowTimeouts(localTurnMillis = 13L),
            runner = runner,
        ) { jsonResponse(LOCAL_JSON) }

        val result = workflows.respond(
            LocalTurnRequest(
                persona = MissionLocal(
                    label = "the teahouse keeper",
                    settingRole = "server",
                    personality = "warm and chatty",
                    voiceProfile = "low and unhurried",
                    pace = "slow",
                ),
                mission = LocalTurnMission(
                    script = listOf(ScriptTurn(pinyin = "ni3 hao3", meaning = "hello", targetTones = listOf(3, 3))),
                ),
                audio = wavClip("learner"),
            ),
        )

        val failure = result.failureOrNull()
        assertThat(failure).isInstanceOf(WorkflowFailure.Timeout::class.java)
        assertThat((failure as WorkflowFailure.Timeout).timeoutMillis).isEqualTo(13L)
        assertThat(runner.seenMillis).containsExactly(13L)
    }

    @Test
    fun `default timeouts match the documented latency budgets`() {
        assertThat(WorkflowTimeouts.PRONUNCIATION_FEEDBACK_TIMEOUT_MS).isEqualTo(2_000L)
        assertThat(WorkflowTimeouts.RESPONSE_TRANSCRIPTION_TIMEOUT_MS).isEqualTo(2_000L)
        assertThat(WorkflowTimeouts.CONVERSATION_TURN_TIMEOUT_MS).isEqualTo(3_000L)
        assertThat(WorkflowTimeouts.MANDARIN_QA_TIMEOUT_MS).isEqualTo(3_000L)
        assertThat(WorkflowTimeouts.LOCAL_TURN_TIMEOUT_MS).isEqualTo(3_000L)
        assertThat(WorkflowTimeouts.EXERCISE_GENERATION_TIMEOUT_MS).isEqualTo(4_000L)
        assertThat(WorkflowTimeouts.FIELD_MISSION_GENERATION_TIMEOUT_MS).isEqualTo(4_000L)
        assertThat(WorkflowTimeouts.SPEECH_SYNTHESIS_TIMEOUT_MS).isEqualTo(10_000L)
        assertThat(WorkflowTimeouts.PROGRESS_SUMMARY_TIMEOUT_MS).isEqualTo(15_000L)
        assertThat(WorkflowTimeouts().pronunciationFeedbackMillis).isEqualTo(2_000L)
        assertThat(WorkflowTimeouts().conversationTurnMillis).isEqualTo(3_000L)
        assertThat(WorkflowTimeouts().localTurnMillis).isEqualTo(3_000L)
        assertThat(WorkflowTimeouts().responseTranscriptionMillis).isEqualTo(2_000L)
    }

    @Test
    fun `each workflow enforces its own default budget`() = runTest {
        val runner = RecordingRunner()
        val workflows = timeoutWorkflows(runner = runner) { jsonResponse(COACHING_JSON) }

        workflows.evaluate(
            PronunciationFeedbackRequest(
                pinyin = "ni3 hao3",
                referenceAudio = wavClip("reference"),
                attemptAudio = wavClip("attempt"),
            ),
        )
        workflows.transcribe(ResponseTranscriptionRequest(audio = wavClip("answer")))
        workflows.respond(ConversationTurnRequest(scenario = "greetings", audio = wavClip("learner")))

        assertThat(runner.seenMillis).containsExactly(2_000L, 2_000L, 3_000L).inOrder()
    }

    @Test
    fun `fast responses still succeed under the timeout`() = runTest {
        val workflows = testWorkflows {
            jsonResponse(COACHING_JSON)
        }

        val result = workflows.evaluate(
            PronunciationFeedbackRequest(
                pinyin = "ni3 hao3",
                referenceAudio = wavClip("reference"),
                attemptAudio = wavClip("attempt"),
            ),
        )

        assertThat(result.valueOrNull()!!.weakestUnit).isEqualTo("hao3")
    }

    /**
     * Simulates an expired budget with the same [withTimeout] call production uses, proving
     * the mapping to [WorkflowResult.Failure] instead of a throw. Runs on virtual time so no
     * real delay elapses.
     */
    private class FakeTimeoutRunner : WorkflowTimeoutRunner {
        val seenMillis = mutableListOf<Long>()

        override suspend fun <T> run(timeoutMillis: Long, block: suspend () -> T): T {
            seenMillis += timeoutMillis
            return withTimeout(timeoutMillis) {
                delay(5_000L)
                block()
            }
        }
    }

    private class RecordingRunner : WorkflowTimeoutRunner {
        val seenMillis = mutableListOf<Long>()

        override suspend fun <T> run(timeoutMillis: Long, block: suspend () -> T): T {
            seenMillis += timeoutMillis
            return block()
        }
    }

    companion object {
        private const val COACHING_JSON =
            """{"weakestUnit":"hao3","issue":"the dip is too shallow","tip":"start lower and dip deeper","encouragement":"good first try","replayHint":"listen for the dip"}"""
        private const val TRANSCRIPT_JSON =
            """{"transcript":"wo3 yao4 yi1 bei1 cha2","matchedOptionId":"option-a","confidence":0.91}"""
        private const val CONVERSATION_JSON =
            """{"replyText":"nin2 xiang3 he1 shen2 me5?","gentleCorrection":"try a smoother third tone","nextPrompt":"ask for the price"}"""
        private const val LOCAL_JSON =
            """{"replyText":"ni3 hao3! jin4 lai2 ba5.","understandingSignal":"smiles and waves","nextLocalPrompt":"ask what the learner wants"}"""
    }
}
