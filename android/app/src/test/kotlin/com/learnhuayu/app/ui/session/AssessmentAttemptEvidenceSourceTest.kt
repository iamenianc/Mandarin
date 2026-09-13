package com.learnhuayu.app.ui.session

import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.app.ui.audio.FakeAudioPlayer
import com.learnhuayu.app.ui.audio.FakeAudioRecorder
import com.learnhuayu.app.ui.audio.FakeRecordingStore
import com.learnhuayu.app.ui.audio.FakeWavCodec
import com.learnhuayu.app.ui.audio.MainDispatcherRule
import com.learnhuayu.core.ai.AudioClip
import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.PronunciationFeedback
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.assessment.ToneAssessmentPipeline
import com.learnhuayu.core.assessment.dsp.BaselineAcousticFeatureExtractor
import com.learnhuayu.core.assessment.evidence.ReferenceFeaturePrecomputer
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.PracticeSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.sin

/**
 * The measured-evidence path behind the `AttemptEvidenceSource` seam (ADR 0014). The tests use
 * fake clip reading and decoding so the real `:core:assessment` pipeline runs on synthetic
 * audio: a two-syllable reference with separated voiced bursts, and an attempt of the same
 * shape. Missing inputs, a failed decode, empty pinyin, and an unvoiced attempt must all yield
 * no evidence and leave the audio-only request untouched.
 */
class AssessmentAttemptEvidenceSourceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun source(
        contentRepository: FakeBundledContentRepository = FakeBundledContentRepository(items = mapOf(item.id to item)),
        reader: FakeReferenceClipReader = readableReader(),
        decoder: FakeReferencePcmDecoder = FakeReferencePcmDecoder(referencePcm),
    ): AssessmentAttemptEvidenceSource = AssessmentAttemptEvidenceSource(
        contentRepository = contentRepository,
        referenceClipReader = reader,
        referencePcmDecoder = decoder,
        extractor = BaselineAcousticFeatureExtractor(),
        precomputer = ReferenceFeaturePrecomputer(),
        pipeline = ToneAssessmentPipeline(),
    )

    @Test
    fun `a measured attempt yields one syllable per expected syllable`() = runTest {
        val evidence = source().evidenceFor(item, attemptPcm)

        assertNotNull(evidence)
        assertEquals("ma1 la2", evidence!!.phrase)
        assertEquals(listOf("ma1", "la2"), evidence.syllables.map { it.pinyin })
        assertEquals(listOf(1, 2), evidence.syllables.map { it.expectedTone })
        assertTrue(evidence.syllables.all { it.observed != null })
        assertTrue(evidence.syllables.all { it.direction != null })
        assertTrue(evidence.syllables.all { it.voicingRatio != null })
    }

    @Test
    fun `reference features are computed once per item across two attempts`() = runTest {
        val decoder = FakeReferencePcmDecoder(referencePcm)
        val evidenceSource = source(decoder = decoder)

        assertNotNull(evidenceSource.evidenceFor(item, attemptPcm))
        assertNotNull(evidenceSource.evidenceFor(item, attemptPcm))

        assertEquals(1, decoder.decodeCount)
    }

    @Test
    fun `a missing reference clip yields no evidence`() = runTest {
        assertNull(source(reader = FakeReferenceClipReader()).evidenceFor(item, attemptPcm))
    }

    @Test
    fun `a decoder failure yields no evidence`() = runTest {
        assertNull(source(decoder = FakeReferencePcmDecoder(null)).evidenceFor(item, attemptPcm))
    }

    @Test
    fun `empty pinyin yields no evidence`() = runTest {
        val noPinyin = item.copy(pinyin = "", targetTones = emptyList())

        assertNull(
            source(contentRepository = FakeBundledContentRepository(items = mapOf(noPinyin.id to noPinyin)))
                .evidenceFor(noPinyin, attemptPcm),
        )
    }

    @Test
    fun `mismatched pinyin and target tones yield no evidence`() = runTest {
        val mismatched = item.copy(targetTones = listOf(1))

        assertNull(
            source(contentRepository = FakeBundledContentRepository(items = mapOf(mismatched.id to mismatched)))
                .evidenceFor(mismatched, attemptPcm),
        )
    }

    @Test
    fun `an unvoiced attempt yields no evidence`() = runTest {
        assertNull(source().evidenceFor(item, silentPcm))
    }

    @Test
    fun `the WF-1 request carries measured evidence for the attempt`() = runTest {
        val workflow = FakePronunciationFeedbackWorkflow(WorkflowResult.Success(feedback))
        val viewModel = viewModel(workflow = workflow, evidenceSource = source())
        viewModel.recordAttempt()

        viewModel.onGetFeedbackClick()
        viewModel.awaitFeedback()

        val evidence = workflow.requests.single().acousticEvidence
        assertNotNull(evidence)
        assertEquals(2, evidence!!.syllables.size)
        assertEquals(listOf(1, 2), evidence.syllables.map { it.expectedTone })
    }

    @Test
    fun `a decoder failure keeps the audio-only request`() = runTest {
        val workflow = FakePronunciationFeedbackWorkflow(WorkflowResult.Success(feedback))
        val viewModel = viewModel(
            workflow = workflow,
            evidenceSource = source(decoder = FakeReferencePcmDecoder(null)),
        )
        viewModel.recordAttempt()

        viewModel.onGetFeedbackClick()
        viewModel.awaitFeedback()

        val request = workflow.requests.single()
        assertNull(request.acousticEvidence)
        assertEquals(FeedbackUiState.Available(feedback), viewModel.uiState.value.feedback)
    }

    @Test
    fun `a missing reference clip shows the offline fallback without calling WF-1`() = runTest {
        val workflow = FakePronunciationFeedbackWorkflow(WorkflowResult.Success(feedback))
        val viewModel = viewModel(
            workflow = workflow,
            evidenceSource = source(reader = FakeReferenceClipReader()),
            reader = FakeReferenceClipReader(),
        )
        viewModel.recordAttempt()

        viewModel.onGetFeedbackClick()
        viewModel.awaitFeedback()

        assertTrue(workflow.requests.isEmpty())
        assertEquals(FeedbackUiState.OfflineFallback, viewModel.uiState.value.feedback)
    }

    private fun viewModel(
        workflow: FakePronunciationFeedbackWorkflow,
        evidenceSource: AttemptEvidenceSource,
        attempt: PcmAudio = attemptPcm,
        reader: FakeReferenceClipReader = readableReader(),
    ): SessionViewModel {
        val recorder = FakeAudioRecorder().apply { stopResult = attempt }
        return SessionViewModel(
            registry = ModuleRegistry(
                setOf(
                    FakeLearningModule(
                        id = MODULE_ID,
                        title = "Speech",
                        practiceSpecs = listOf(speakAndRepeatSpec),
                    ),
                ),
            ),
            contentRepository = FakeBundledContentRepository(items = mapOf(item.id to item)),
            audioPlayer = FakeAudioPlayer(),
            audioRecorder = recorder,
            wavCodec = FakeWavCodec(),
            recordingStore = FakeRecordingStore(attemptFile),
            progressRepository = FakeProgressRepository(),
            attemptRepository = FakeAttemptRepository(),
            preferencesRepository = FakePreferencesRepository(),
            feedbackWorkflow = workflow,
            responseWorkflow = FakeResponseTranscriptionWorkflow(),
            referenceClipReader = reader,
            evidenceSource = evidenceSource,
            clock = fixedClock,
        )
    }

    private fun SessionViewModel.recordAttempt() {
        onPermissionStatusChecked(true)
        load(MODULE_ID, "practice", speakAndRepeatSpec.id)
        onRecordClick()
        onRecordClick()
    }

    private suspend fun SessionViewModel.awaitFeedback(): FeedbackUiState = uiState.first { it.feedback !is FeedbackUiState.None && it.feedback !is FeedbackUiState.Loading }.feedback

    private fun readableReader(): FakeReferenceClipReader = FakeReferenceClipReader(mapOf(REFERENCE_ASSET_PATH to AudioClip(referenceBytes, AudioFormat.OGG)))

    private companion object {
        const val MODULE_ID = "speech"
        val recordedAt: Instant = Instant.parse("2026-09-13T09:00:00Z")
        val fixedClock: Clock = Clock.fixed(recordedAt, ZoneOffset.UTC)
        val attemptFile = File("build/tmp/assessment-attempt-evidence-test/attempt.wav")

        val item = ContentItem(
            id = "speech-ma1-la2",
            type = ContentItemType.PHRASE,
            source = ContentSource.BUNDLED,
            meaning = "test phrase",
            audioAssetRef = "reference/speech/speech-ma1-la2.ogg",
            pinyin = "ma1 la2",
            targetTones = listOf(1, 2),
        )
        const val REFERENCE_ASSET_PATH = "audio/reference/speech/speech-ma1-la2.ogg"
        val referenceBytes = byteArrayOf(1, 2, 3, 4)

        const val SAMPLE_RATE_HZ = 24_000

        val referencePcm = toneClip(
            Sweep(startHz = 150f, endHz = 150f, durationMs = 250),
            Sweep(startHz = 0f, endHz = 0f, durationMs = 200, silent = true),
            Sweep(startHz = 130f, endHz = 190f, durationMs = 250),
        )
        val attemptPcm = toneClip(
            Sweep(startHz = 150f, endHz = 150f, durationMs = 250),
            Sweep(startHz = 0f, endHz = 0f, durationMs = 200, silent = true),
            Sweep(startHz = 130f, endHz = 190f, durationMs = 250),
        )
        val silentPcm = PcmAudio(ShortArray(SAMPLE_RATE_HZ * 700 / 1000), SAMPLE_RATE_HZ)

        val feedback = PronunciationFeedback(
            weakestUnit = "ma1",
            issue = "the tone is flat",
            tip = "start higher",
            encouragement = "good effort",
            replayHint = "listen to ma1 again",
        )

        val speakAndRepeatSpec = PracticeSpec(
            id = "speech-practice-say-the-tone",
            moduleId = MODULE_ID,
            title = "Say the tone practice",
            contentItemIds = listOf(item.id),
            mode = DrillMode.SPEAK_AND_REPEAT_FEEDBACK,
        )
    }
}

private data class Sweep(
    val startHz: Float,
    val endHz: Float,
    val durationMs: Int,
    val silent: Boolean = false,
)

private fun toneClip(vararg sweeps: Sweep): PcmAudio {
    val sampleRateHz = 24_000
    val total = sweeps.sumOf { sampleRateHz * it.durationMs / 1000 }
    val samples = ShortArray(total)
    var offset = 0
    for (sweep in sweeps) {
        val count = sampleRateHz * sweep.durationMs / 1000
        if (!sweep.silent) {
            var phase = 0.0
            for (index in 0 until count) {
                val fraction = if (count <= 1) 1.0 else index.toDouble() / (count - 1)
                val frequency = sweep.startHz + (sweep.endHz - sweep.startHz) * fraction
                phase += 2.0 * PI * frequency / sampleRateHz
                samples[offset + index] = (0.4f * sin(phase) * Short.MAX_VALUE).toInt().toShort()
            }
        }
        offset += count
    }
    return PcmAudio(samples, sampleRateHz)
}
