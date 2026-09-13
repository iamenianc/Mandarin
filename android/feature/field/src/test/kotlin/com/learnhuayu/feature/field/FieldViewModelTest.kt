package com.learnhuayu.feature.field

import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.FieldMissionGenerationRequest
import com.learnhuayu.core.ai.FieldMissionGenerationWorkflow
import com.learnhuayu.core.ai.GeneratedMission
import com.learnhuayu.core.ai.LocalTurnReply
import com.learnhuayu.core.ai.LocalTurnRequest
import com.learnhuayu.core.ai.LocalTurnWorkflow
import com.learnhuayu.core.ai.MissionLocal
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
import com.learnhuayu.core.data.repository.DebriefEntryRepository
import com.learnhuayu.core.data.repository.FieldMissionRepository
import com.learnhuayu.core.data.repository.LocalPersonaRepository
import com.learnhuayu.core.data.repository.MissionSessionRepository
import com.learnhuayu.core.data.repository.MissionTurnRepository
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.DebriefEntry
import com.learnhuayu.core.model.DebriefKind
import com.learnhuayu.core.model.FieldMission
import com.learnhuayu.core.model.LocalPersona
import com.learnhuayu.core.model.MissionSession
import com.learnhuayu.core.model.MissionTurn
import com.learnhuayu.core.model.ScriptTurn
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
 * Covers the daily LAMP field loop view model with fakes for WF-9, WF-10, WF-3 and the five
 * repositories: generation success, generation failure falling back to the bundled mission,
 * progression through all five locals, in-mission replies carrying no coaching, debrief entry
 * persistence, and the TTS-unavailable path.
 */
class FieldViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val generation = FakeGenerationWorkflow()
    private val turns = FakeTurnWorkflow()
    private val speech = FakeSpeechWorkflow()
    private val player = FakeFieldAudioPlayer()
    private val missions = FakeMissionRepository()
    private val personas = FakePersonaRepository()
    private val sessions = FakeSessionRepository()
    private val missionTurns = FakeMissionTurnRepository()
    private val debrief = FakeDebriefRepository()
    private val recorder = FakeFieldRecorder()
    private val wavCodec = FakeFieldWavCodec()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-13T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `generation success persists the mission and the five personas`() {
        generation.result = WorkflowResult.Success(TEST_GENERATED)
        val viewModel = viewModel()

        viewModel.startRehearsal("market greeting")

        val state = viewModel.uiState.value
        assertFalse(state.generating)
        assertNull(state.errorMessage)
        assertEquals(FieldStep.Rehearsal, state.step)
        assertEquals(ContentSource.GENERATED, state.mission?.source)
        assertEquals(5, state.locals.size)
        assertEquals(state.mission, missions.upserted.single())
        assertEquals(5, personas.upserted.size)
        assertTrue(state.canStartMission)
    }

    @Test
    fun `generation failure falls back to the bundled mission`() {
        generation.result = WorkflowResult.Failure(WorkflowFailure.NetworkError("offline"))
        val viewModel = viewModel()

        viewModel.startRehearsal("market greeting")

        val state = viewModel.uiState.value
        assertFalse(state.generating)
        assertEquals(FIELD_OFFLINE_MESSAGE, state.errorMessage)
        assertEquals(ContentSource.BUNDLED, state.mission?.source)
        assertEquals(BundledFieldMission.script, state.mission?.scriptTurns)
        assertEquals(5, state.locals.size)
        assertEquals(BundledFieldMission.locals, state.locals)
        assertEquals(state.mission, missions.upserted.single())
        assertEquals(5, personas.upserted.size)
        assertTrue(state.canStartMission)
    }

    @Test
    fun `a mission walks all five locals then opens the debrief`() {
        generation.result = WorkflowResult.Success(TEST_GENERATED)
        val viewModel = viewModel()
        viewModel.startRehearsal("market greeting")
        viewModel.onPermissionResult(granted = true)

        viewModel.onStartMission()
        assertEquals(FieldStep.Mission, viewModel.uiState.value.step)
        assertEquals("local 1 of 5", viewModel.uiState.value.localProgressLabel)

        repeat(5) { index ->
            if (index > 0) {
                assertEquals(index, viewModel.uiState.value.localIndex)
                assertEquals(0, viewModel.uiState.value.exchanges.size)
            }
            viewModel.onRecordClick()
            viewModel.onRecordClick()
            assertEquals(1, viewModel.uiState.value.exchanges.size)
            viewModel.onNextLocal()
        }

        val state = viewModel.uiState.value
        assertEquals(FieldStep.Debrief, state.step)
        assertEquals(5, turns.requests.size)
        val created = sessions.upserted.filter { it.endedAt == null }
        assertEquals(5, created.size)
        assertEquals(5, sessions.upserted.filter { it.endedAt != null }.size)
        assertEquals(10, missionTurns.upserted.size)
    }

    @Test
    fun `in-mission replies stay in character with no coaching`() {
        generation.result = WorkflowResult.Success(TEST_GENERATED)
        val viewModel = viewModel()
        viewModel.startRehearsal("market greeting")
        viewModel.onPermissionResult(granted = true)
        viewModel.onStartMission()

        viewModel.onRecordClick()
        viewModel.onRecordClick()

        val reply = viewModel.uiState.value.exchanges.single().reply
        assertEquals("hao3 de5, gei3 ni3", reply.replyText)
        assertNull(reply.understandingSignal)
        assertNull(reply.nextLocalPrompt)
        val request = turns.requests.single()
        assertEquals("Test Seller 0", request.persona.label)
        assertEquals(TEST_GENERATED.script, request.mission.script)
        assertEquals(AudioFormat.WAV, request.audio.format)
        assertTrue(request.audio.bytes.isNotEmpty())
    }

    @Test
    fun `debrief entries persist tied to the mission`() {
        generation.result = WorkflowResult.Success(TEST_GENERATED)
        val viewModel = viewModel()
        viewModel.startRehearsal("market greeting")
        viewModel.onPermissionResult(granted = true)
        viewModel.onStartMission()
        repeat(5) { _ ->
            viewModel.onRecordClick()
            viewModel.onRecordClick()
            viewModel.onNextLocal()
        }
        assertEquals(FieldStep.Debrief, viewModel.uiState.value.step)

        viewModel.onAddDebriefEntry("zhe4 ge5", DebriefKind.UNFAMILIAR_WORD, "did not catch")
        viewModel.onAddDebriefEntry("duo1 shao3", DebriefKind.TONE_BREAKDOWN, null)

        val entries = viewModel.uiState.value.debriefEntries
        assertEquals(2, entries.size)
        assertEquals(2, debrief.upserted.size)
        val missionId = viewModel.uiState.value.mission?.id
        assertTrue(debrief.upserted.all { it.missionId == missionId })
        assertEquals("zhe4 ge5", entries[0].pinyin)
        assertEquals("duo1 shao3", entries[1].pinyin)
        viewModel.onFinishDebrief()
        assertTrue(viewModel.uiState.value.finished)
    }

    @Test
    fun `tts failure still shows the reply text without audio`() {
        speech.result = WorkflowResult.Failure(WorkflowFailure.NetworkError("offline"))
        generation.result = WorkflowResult.Success(TEST_GENERATED)
        val viewModel = viewModel()
        viewModel.startRehearsal("market greeting")
        viewModel.onPermissionResult(granted = true)
        viewModel.onStartMission()

        viewModel.onRecordClick()
        viewModel.onRecordClick()

        val exchange = viewModel.uiState.value.exchanges.single()
        assertEquals("hao3 de5, gei3 ni3", exchange.reply.replyText)
        assertFalse(exchange.audioAvailable)
        assertTrue(player.played.isEmpty())
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `player failure still keeps the reply and skips audio only`() {
        player.fail = true
        generation.result = WorkflowResult.Success(TEST_GENERATED)
        val viewModel = viewModel()
        viewModel.startRehearsal("market greeting")
        viewModel.onPermissionResult(granted = true)
        viewModel.onStartMission()

        viewModel.onRecordClick()
        viewModel.onRecordClick()

        val exchange = viewModel.uiState.value.exchanges.single()
        assertEquals("hao3 de5, gei3 ni3", exchange.reply.replyText)
        assertFalse(exchange.audioAvailable)
    }

    private fun viewModel(): FieldViewModel = FieldViewModel(
        generationWorkflow = generation,
        turnWorkflow = turns,
        speechWorkflow = speech,
        fieldAudioPlayer = player,
        missionRepository = missions,
        personaRepository = personas,
        sessionRepository = sessions,
        turnRepository = missionTurns,
        debriefRepository = debrief,
        audioRecorder = recorder,
        wavCodec = wavCodec,
        clock = clock,
    )

    private companion object {
        val TEST_GENERATED = GeneratedMission(
            script = listOf(
                ScriptTurn(pinyin = "ni3 hao3", meaning = "hello", targetTones = listOf(3, 3)),
                ScriptTurn(pinyin = "xie4 xie5", meaning = "thanks", targetTones = listOf(4, 5)),
            ),
            locals = List(5) { index ->
                MissionLocal(
                    label = "Test Seller $index",
                    settingRole = "test role $index",
                    personality = "test personality $index",
                    voiceProfile = "test voice $index",
                    pace = "medium",
                )
            },
        )
    }
}

private class FakeGenerationWorkflow(
    var result: WorkflowResult<GeneratedMission> = WorkflowResult.Failure(
        WorkflowFailure.NetworkError("unset"),
    ),
) : FieldMissionGenerationWorkflow {

    val requests = mutableListOf<FieldMissionGenerationRequest>()

    override suspend fun generate(request: FieldMissionGenerationRequest): WorkflowResult<GeneratedMission> {
        requests += request
        return result
    }
}

private class FakeTurnWorkflow(
    var result: WorkflowResult<LocalTurnReply> = WorkflowResult.Success(
        LocalTurnReply(replyText = "hao3 de5, gei3 ni3"),
    ),
) : LocalTurnWorkflow {

    val requests = mutableListOf<LocalTurnRequest>()

    override suspend fun respond(request: LocalTurnRequest): WorkflowResult<LocalTurnReply> {
        requests += request
        return result
    }
}

private class FakeSpeechWorkflow(
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

private class FakeFieldAudioPlayer(var fail: Boolean = false) : FieldAudioPlayer {

    val played = mutableListOf<ByteArray>()

    override suspend fun playSpeech(bytes: ByteArray, contentType: String?) {
        if (fail) error("player unavailable")
        played += bytes
    }
}

private class FakeMissionRepository : FieldMissionRepository {

    val upserted = mutableListOf<FieldMission>()
    private val stored = mutableMapOf<String, FieldMission>()

    override suspend fun upsert(mission: FieldMission) {
        upserted += mission
        stored[mission.id] = mission
    }

    override suspend fun byId(id: String): FieldMission? = stored[id]

    override fun all(): Flow<List<FieldMission>> = flowOf(stored.values.toList())

    override suspend fun deleteById(id: String) {
        stored.remove(id)
    }
}

private class FakePersonaRepository : LocalPersonaRepository {

    val upserted = mutableListOf<LocalPersona>()
    private val stored = mutableMapOf<String, LocalPersona>()

    override suspend fun upsert(persona: LocalPersona) {
        upserted += persona
        stored[persona.id] = persona
    }

    override suspend fun byId(id: String): LocalPersona? = stored[id]

    override fun all(): Flow<List<LocalPersona>> = flowOf(stored.values.toList())

    override suspend fun deleteById(id: String) {
        stored.remove(id)
    }
}

private class FakeSessionRepository : MissionSessionRepository {

    val upserted = mutableListOf<MissionSession>()
    private val stored = mutableMapOf<String, MissionSession>()

    override suspend fun upsert(session: MissionSession) {
        upserted += session
        stored[session.id] = session
    }

    override suspend fun byId(id: String): MissionSession? = stored[id]

    override fun byMission(missionId: String): Flow<List<MissionSession>> = flowOf(stored.values.filter { it.missionId == missionId })

    override suspend fun deleteById(id: String) {
        stored.remove(id)
    }
}

private class FakeMissionTurnRepository : MissionTurnRepository {

    val upserted = mutableListOf<MissionTurn>()
    private val stored = mutableMapOf<String, MissionTurn>()

    override suspend fun upsert(turn: MissionTurn) {
        upserted += turn
        stored[turn.id] = turn
    }

    override suspend fun byId(id: String): MissionTurn? = stored[id]

    override fun bySession(sessionId: String): Flow<List<MissionTurn>> = flowOf(stored.values.filter { it.sessionId == sessionId })

    override suspend fun deleteById(id: String) {
        stored.remove(id)
    }
}

private class FakeDebriefRepository : DebriefEntryRepository {

    val upserted = mutableListOf<DebriefEntry>()
    private val stored = mutableMapOf<String, DebriefEntry>()

    override suspend fun upsert(entry: DebriefEntry) {
        upserted += entry
        stored[entry.id] = entry
    }

    override suspend fun byId(id: String): DebriefEntry? = stored[id]

    override fun byMission(missionId: String): Flow<List<DebriefEntry>> = flowOf(stored.values.filter { it.missionId == missionId })

    override fun all(): Flow<List<DebriefEntry>> = flowOf(stored.values.toList())

    override suspend fun deleteById(id: String) {
        stored.remove(id)
    }
}

private class FakeFieldRecorder : AudioRecorder {

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

private class FakeFieldWavCodec : WavCodec {
    override fun encode(audio: PcmAudio): ByteArray = byteArrayOf(1, 2, 3, 4)

    override fun decode(bytes: ByteArray): PcmAudio = PcmAudio(ShortArray(0), 24_000)
}
