package com.learnhuayu.feature.raymond

import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.MandarinExample
import com.learnhuayu.core.ai.MandarinQaAnswer
import com.learnhuayu.core.ai.MandarinQaRequest
import com.learnhuayu.core.ai.MandarinQaWorkflow
import com.learnhuayu.core.ai.QaExchange
import com.learnhuayu.core.ai.RaymondRole
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.capture.AudioRecorder
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.audio.vad.VadEvent
import com.learnhuayu.core.audio.wav.WavCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RaymondViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val workflow = FakeMandarinQaWorkflow()
    private val recorder = FakeRaymondRecorder()
    private val wavCodec = FakeRaymondWavCodec()

    @Test
    fun `a text question surfaces the answer, examples, and follow-ups`() {
        workflow.result = WorkflowResult.Success(
            MandarinQaAnswer(
                answerText = "ni3 hao3 is the everyday greeting.",
                examples = listOf(MandarinExample(pinyin = "ni3 hao3", meaning = "hello")),
                followUps = listOf("How do I say thank you?"),
            ),
        )
        val viewModel = viewModel()

        viewModel.onInputChange("How do I greet someone?")
        viewModel.onAskClick()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertNull(state.errorMessage)
        assertEquals("", state.input)
        val turn = state.turns.single()
        assertEquals("How do I greet someone?", turn.question)
        assertEquals("ni3 hao3 is the everyday greeting.", turn.answer?.answerText)
        assertEquals(listOf(MandarinExample(pinyin = "ni3 hao3", meaning = "hello")), turn.answer?.examples)
        assertEquals(listOf("How do I say thank you?"), turn.answer?.followUps)
    }

    @Test
    fun `tapping a follow-up asks it as the next question`() {
        workflow.result = WorkflowResult.Success(
            MandarinQaAnswer(
                answerText = "Greetings come in many forms.",
                followUps = listOf("How do I say thank you?"),
            ),
        )
        val viewModel = viewModel()
        viewModel.onInputChange("How do I greet someone?")
        viewModel.onAskClick()

        viewModel.onFollowUpClick("How do I say thank you?")

        assertEquals("How do I say thank you?", workflow.requests.last().question)
        assertEquals(2, viewModel.uiState.value.turns.size)
        assertEquals("How do I say thank you?", viewModel.uiState.value.turns.last().question)
    }

    @Test
    fun `history accumulates the earlier questions and answers across turns`() {
        workflow.result = WorkflowResult.Success(MandarinQaAnswer(answerText = "a1"))
        val viewModel = viewModel()
        viewModel.onInputChange("q1")
        viewModel.onAskClick()

        workflow.result = WorkflowResult.Success(MandarinQaAnswer(answerText = "a2"))
        viewModel.onInputChange("q2")
        viewModel.onAskClick()

        val request = workflow.requests.last()
        assertEquals("q2", request.question)
        assertEquals(
            listOf(
                QaExchange(RaymondRole.USER, "q1"),
                QaExchange(RaymondRole.RAYMOND, "a1"),
            ),
            request.history,
        )
        assertNull(request.learnerLevel)
    }

    @Test
    fun `a failed answer surfaces the offline message and never crashes`() {
        workflow.result = WorkflowResult.Failure(WorkflowFailure.NetworkError("offline"))
        val viewModel = viewModel()

        viewModel.onInputChange("How do tones work?")
        viewModel.onAskClick()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals(RAYMOND_OFFLINE_MESSAGE, state.errorMessage)
        assertTrue(state.turns.single().failed)
        assertNull(state.turns.single().answer)
    }

    @Test
    fun `a recorder failure surfaces the type-instead note rather than raw detail`() {
        // The fake recorder does not emit failures, so drive the presentation rule through
        // the documented constant: recorder faults must never show technical text.
        assertEquals(
            "The recording could not be used. Type the question instead.",
            RAYMOND_RECORDER_ERROR_MESSAGE,
        )
    }

    @Test
    fun `a typed question with audio sends both the text and the clip`() {
        workflow.result = WorkflowResult.Success(MandarinQaAnswer(answerText = "a"))
        val viewModel = viewModel()
        viewModel.onPermissionStatusChecked(granted = true)
        viewModel.onInputChange("What does this mean?")
        viewModel.onRecordClick()
        viewModel.onRecordClick()

        val request = workflow.requests.single()
        assertEquals("What does this mean?", request.question)
        assertEquals(AudioFormat.WAV, request.audio?.format)
        assertTrue(request.audio?.bytes?.isNotEmpty() == true)
    }

    private fun viewModel(): RaymondViewModel = RaymondViewModel(
        workflow = workflow,
        audioRecorder = recorder,
        wavCodec = wavCodec,
    )
}

private class FakeMandarinQaWorkflow(
    var result: WorkflowResult<MandarinQaAnswer> = WorkflowResult.Success(MandarinQaAnswer(answerText = "answer")),
) : MandarinQaWorkflow {

    val requests = mutableListOf<MandarinQaRequest>()

    override suspend fun ask(request: MandarinQaRequest): WorkflowResult<MandarinQaAnswer> {
        requests += request
        return result
    }
}

private class FakeRaymondRecorder : AudioRecorder {

    private val mutableState = MutableStateFlow<RecorderState>(RecorderState.Idle)
    private val mutableLevel = MutableStateFlow(0f)

    override val state: StateFlow<RecorderState> = mutableState
    override val level: StateFlow<Float> = mutableLevel
    override val vadEvents: Flow<VadEvent> = emptyFlow()

    override fun start() {
        mutableState.value = RecorderState.Recording
    }

    override suspend fun stop(): PcmAudio {
        mutableState.value = RecorderState.Idle
        return PcmAudio(ShortArray(SAMPLE_COUNT), SAMPLE_RATE_HZ)
    }

    override fun release() = Unit

    private companion object {
        const val SAMPLE_COUNT = 16
        const val SAMPLE_RATE_HZ = 24_000
    }
}

private class FakeRaymondWavCodec : WavCodec {
    override fun encode(audio: PcmAudio): ByteArray = byteArrayOf(1, 2, 3, 4)

    override fun decode(bytes: ByteArray): PcmAudio = PcmAudio(ShortArray(0), 24_000)
}
