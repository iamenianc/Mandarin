package com.learnhuayu.feature.conversation

import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.ConversationTurnReply
import com.learnhuayu.core.ai.ConversationTurnRequest
import com.learnhuayu.core.ai.ConversationTurnWorkflow
import com.learnhuayu.core.ai.SpeechSynthesisRequest
import com.learnhuayu.core.ai.SpeechSynthesisWorkflow
import com.learnhuayu.core.ai.SynthesizedSpeech
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.audio.capture.AudioRecorder
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.audio.vad.VadEvent
import com.learnhuayu.core.audio.wav.WavCodec
import com.learnhuayu.core.data.repository.ConversationSessionRepository
import com.learnhuayu.core.data.repository.TurnRepository
import com.learnhuayu.core.model.ConversationSession
import com.learnhuayu.core.model.ConversationSpeaker
import com.learnhuayu.core.model.Turn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Covers the coaching conversation view model with fakes for WF-2, WF-3 and the two
 * repositories: the happy path rendering replyText with correction and prompt, the WF-2
 * failure falling back to a scripted line, the WF-3 501 keeping text without audio, and
 * consecutive offline turns cycling scripted lines so the exchange keeps moving.
 */
class ConversationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val turns = FakeConversationTurnWorkflow()
    private val speech = FakeConversationSpeechWorkflow()
    private val player = FakeConversationAudioPlayer()
    private val sessions = FakeConversationSessionRepository()
    private val conversationTurns = FakeConversationTurnRepository()
    private val recorder = FakeConversationRecorder()
    private val wavCodec = FakeConversationWavCodec()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `happy path renders the reply correction and next prompt with audio`() {
        turns.result = WorkflowResult.Success(
            ConversationTurnReply(
                replyText = "hen3 hao3",
                gentleCorrection = "hao3 dips low, then rises.",
                nextPrompt = "Ask ni3 hao3 ma5?",
            ),
        )
        val viewModel = viewModel()
        viewModel.onScenarioSelect("greeting")
        viewModel.onPermissionResult(granted = true)

        viewModel.onRecordClick()
        viewModel.onRecordClick()

        val state = viewModel.uiState.value
        assertFalse(state.sending)
        assertNull(state.errorMessage)
        assertEquals(2, state.exchanges.size)
        val exchange = state.exchanges.last()
        assertEquals("hen3 hao3", exchange.reply.replyText)
        assertEquals("hao3 dips low, then rises.", exchange.reply.gentleCorrection)
        assertEquals("Ask ni3 hao3 ma5?", exchange.reply.nextPrompt)
        assertTrue(exchange.audioAvailable)
        assertFalse(exchange.scripted)
        val request = turns.requests.single()
        assertEquals("greeting", request.scenario)
        assertEquals("beginner", request.targetDifficulty)
        assertEquals(AudioFormat.WAV, request.audio.format)
        assertTrue(request.audio.bytes.isNotEmpty())
        assertTrue(request.conversationState?.transcripts?.isNotEmpty() == true)
        assertEquals(2, conversationTurns.upserted.count { it.speaker == ConversationSpeaker.AI })
        assertEquals(1, conversationTurns.upserted.count { it.speaker == ConversationSpeaker.USER })
    }

    @Test
    fun `wf-2 failure falls back to a scripted line and keeps the exchange moving`() {
        turns.result = WorkflowResult.Failure(WorkflowFailure.NetworkError("offline"))
        val viewModel = viewModel()
        viewModel.onScenarioSelect("greeting")
        viewModel.onPermissionResult(granted = true)

        viewModel.onRecordClick()
        viewModel.onRecordClick()

        val state = viewModel.uiState.value
        assertFalse(state.sending)
        assertEquals(CONVERSATION_OFFLINE_MESSAGE, state.errorMessage)
        assertEquals(2, state.exchanges.size)
        val exchange = state.exchanges.last()
        assertTrue(exchange.scripted)
        assertTrue(exchange.reply.replyText.isNotBlank())
        assertEquals(2, conversationTurns.upserted.count { it.speaker == ConversationSpeaker.AI })
    }

    @Test
    fun `wf-3 501 keeps the reply text without audio`() {
        speech.result = WorkflowResult.Failure(WorkflowFailure.ProviderNotConfigured("tts not configured"))
        turns.result = WorkflowResult.Success(
            ConversationTurnReply(
                replyText = "hen3 hao3",
                gentleCorrection = "hao3 dips low.",
                nextPrompt = "Repeat once more.",
            ),
        )
        val viewModel = viewModel()
        viewModel.onScenarioSelect("greeting")
        viewModel.onPermissionResult(granted = true)

        viewModel.onRecordClick()
        viewModel.onRecordClick()

        val exchange = viewModel.uiState.value.exchanges.last()
        assertEquals("hen3 hao3", exchange.reply.replyText)
        assertFalse(exchange.audioAvailable)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `consecutive offline turns cycle scripted lines`() {
        turns.result = WorkflowResult.Failure(WorkflowFailure.NetworkError("offline"))
        val viewModel = viewModel()
        viewModel.onScenarioSelect("greeting")
        viewModel.onPermissionResult(granted = true)

        viewModel.onRecordClick()
        viewModel.onRecordClick()
        viewModel.onRecordClick()
        viewModel.onRecordClick()

        val replies = viewModel.uiState.value.exchanges.drop(1).map { it.reply.replyText }
        assertEquals(2, replies.size)
        assertTrue(replies.all { it.isNotBlank() })
        assertTrue(replies[0] != replies[1])
        assertTrue(viewModel.uiState.value.exchanges.drop(1).all { it.scripted })
        assertEquals(CONVERSATION_OFFLINE_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    private fun viewModel(): ConversationViewModel = ConversationViewModel(
        turnWorkflow = turns,
        speechWorkflow = speech,
        conversationAudioPlayer = player,
        sessionRepository = sessions,
        turnRepository = conversationTurns,
        audioRecorder = recorder,
        wavCodec = wavCodec,
        clock = clock,
    )
}

private class FakeConversationTurnWorkflow(
    var result: WorkflowResult<ConversationTurnReply> = WorkflowResult.Success(
        ConversationTurnReply(replyText = "hen3 hao3"),
    ),
) : ConversationTurnWorkflow {

    val requests = mutableListOf<ConversationTurnRequest>()

    override suspend fun respond(request: ConversationTurnRequest): WorkflowResult<ConversationTurnReply> {
        requests += request
        return result
    }
}

private class FakeConversationSpeechWorkflow(
    var result: WorkflowResult<SynthesizedSpeech> = WorkflowResult.Success(
        SynthesizedSpeech(bytes = byteArrayOf(1, 2, 3), contentType = "audio/wav"),
    ),
) : SpeechSynthesisWorkflow {

    val requests = mutableListOf<SpeechSynthesisRequest>()

    override suspend fun synthesize(request: SpeechSynthesisRequest): WorkflowResult<SynthesizedSpeech> {
        requests += request
        return result
    }
}

private class FakeConversationAudioPlayer(var fail: Boolean = false) : ConversationAudioPlayer {

    val played = mutableListOf<ByteArray>()

    override suspend fun playSpeech(bytes: ByteArray, contentType: String?) {
        if (fail) error("player unavailable")
        played += bytes
    }
}

private class FakeConversationSessionRepository : ConversationSessionRepository {

    val upserted = mutableListOf<ConversationSession>()
    private val stored = mutableMapOf<String, ConversationSession>()

    override suspend fun upsert(session: ConversationSession) {
        upserted += session
        stored[session.id] = session
    }

    override suspend fun byId(id: String): ConversationSession? = stored[id]

    override fun all(): Flow<List<ConversationSession>> = flowOf(stored.values.toList())

    override suspend fun deleteById(id: String) {
        stored.remove(id)
    }
}

private class FakeConversationTurnRepository : TurnRepository {

    val upserted = mutableListOf<Turn>()
    private val stored = mutableMapOf<String, Turn>()

    override suspend fun upsert(turn: Turn) {
        upserted += turn
        stored[turn.id] = turn
    }

    override suspend fun byId(id: String): Turn? = stored[id]

    override fun bySession(sessionId: String): Flow<List<Turn>> = flowOf(stored.values.filter { it.sessionId == sessionId })

    override suspend fun deleteById(id: String) {
        stored.remove(id)
    }
}

private class FakeConversationRecorder : AudioRecorder {

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
        return PcmAudio(ShortArray(16), 24_000)
    }

    override fun release() = Unit
}

private class FakeConversationWavCodec : WavCodec {
    override fun encode(audio: PcmAudio): ByteArray = byteArrayOf(1, 2, 3, 4)

    override fun decode(bytes: ByteArray): PcmAudio = PcmAudio(ShortArray(0), 24_000)
}
