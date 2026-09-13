package com.learnhuayu.core.assessment.segment

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.assessment.PcmAudio
import com.learnhuayu.core.assessment.SyntheticAudio
import com.learnhuayu.core.assessment.dsp.BaselineAcousticFeatureExtractor
import com.learnhuayu.core.assessment.dsp.FeatureContour
import com.learnhuayu.core.assessment.dsp.FrameFeature
import org.junit.Test

class SyllableAutoSegmenterTest {

    private val extractor = BaselineAcousticFeatureExtractor()

    private fun extract(pcm: PcmAudio): FeatureContour = extractor.extract(pcm)!!

    private fun tone(startHz: Float, endHz: Float, durationMs: Int): PcmAudio = SyntheticAudio.contour(
        listOf(SyntheticAudio.ContourSegment(startHz = startHz, endHz = endHz, durationMs = durationMs)),
    )

    private fun silence(durationMs: Int): PcmAudio = PcmAudio(FloatArray(SyntheticAudio.SAMPLE_RATE * durationMs / 1000), SyntheticAudio.SAMPLE_RATE)

    private fun combine(vararg clips: PcmAudio): PcmAudio {
        val sampleRateHz = clips.first().sampleRateHz
        val total = clips.sumOf { it.samples.size }
        val samples = FloatArray(total)
        var offset = 0
        for (clip in clips) {
            clip.samples.copyInto(samples, offset)
            offset += clip.samples.size
        }
        return PcmAudio(samples, sampleRateHz)
    }

    private fun dip(pcm: PcmAudio, centersMs: List<Int>, widthMs: Int, factor: Float): PcmAudio {
        val samples = pcm.samples.copyOf()
        val half = widthMs * pcm.sampleRateHz / 1000 / 2
        for (centerMs in centersMs) {
            val center = centerMs * pcm.sampleRateHz / 1000
            val from = (center - half).coerceAtLeast(0)
            val to = (center + half).coerceAtMost(samples.size - 1)
            for (index in from..to) {
                samples[index] *= factor
            }
        }
        return PcmAudio(samples, pcm.sampleRateHz)
    }

    private fun contourOf(voicedRms: List<Pair<Boolean, Float>>): FeatureContour = FeatureContour(
        frames = voicedRms.mapIndexed { index, (voiced, rms) ->
            FrameFeature(
                index = index,
                timeMs = index * HOP_MS,
                f0Hz = if (voiced) 150f else null,
                clarity = if (voiced) 0.9f else 0f,
                voiced = voiced,
                rms = rms,
            )
        },
        sampleRateHz = 16_000,
        frameLengthMs = 40,
        hopMs = HOP_MS,
    )

    @Test
    fun `one syllable yields one boundary`() {
        val contour = extract(tone(startHz = 150f, endHz = 150f, durationMs = 400))

        val boundaries = SyllableAutoSegmenter.segment(contour, listOf(1))

        assertThat(boundaries).hasSize(1)
        assertThat(boundaries!!.single().expectedTone).isEqualTo(1)
        assertThat(boundaries.single().startMs).isEqualTo(0)
        assertThat(boundaries.single().endMs).isGreaterThan(0)
    }

    @Test
    fun `two voiced bursts yield two boundaries`() {
        val contour = extract(
            combine(
                tone(startHz = 130f, endHz = 190f, durationMs = 250),
                silence(durationMs = 200),
                tone(startHz = 190f, endHz = 120f, durationMs = 250),
            ),
        )

        val boundaries = SyllableAutoSegmenter.segment(contour, listOf(2, 4))

        assertThat(boundaries).hasSize(2)
        assertThat(boundaries!!.map { it.expectedTone }).containsExactly(2, 4).inOrder()
        assertThat(boundaries[1].startMs).isAtLeast(boundaries[0].endMs)
        assertThat(boundaries[0].endMs).isAtMost(300)
    }

    @Test
    fun `three syllables split at the energy valleys`() {
        val contour = extract(
            dip(
                pcm = tone(startHz = 150f, endHz = 150f, durationMs = 600),
                centersMs = listOf(200, 400),
                widthMs = 60,
                factor = 0.1f,
            ),
        )

        val boundaries = SyllableAutoSegmenter.segment(contour, listOf(1, 1, 1))

        assertThat(boundaries).hasSize(3)
        assertThat(boundaries!!.map { it.expectedTone }).containsExactly(1, 1, 1).inOrder()
        assertThat(boundaries[1].startMs).isIn(140..260)
        assertThat(boundaries[2].startMs).isIn(340..460)
    }

    @Test
    fun `the neutral tone is carried into every boundary`() {
        val contour = extract(
            combine(
                tone(startHz = 130f, endHz = 190f, durationMs = 250),
                silence(durationMs = 200),
                tone(startHz = 190f, endHz = 120f, durationMs = 250),
            ),
        )

        val boundaries = SyllableAutoSegmenter.segment(contour, listOf(5, 5))

        assertThat(boundaries).hasSize(2)
        assertThat(boundaries!!.map { it.expectedTone }).containsExactly(5, 5).inOrder()
    }

    @Test
    fun `a fully unvoiced clip yields no boundaries`() {
        val contour = extract(silence(durationMs = 500))

        assertThat(SyllableAutoSegmenter.segment(contour, listOf(1, 2))).isNull()
    }

    @Test
    fun `an empty or invalid tone sequence yields no boundaries`() {
        val contour = extract(tone(startHz = 150f, endHz = 150f, durationMs = 400))

        assertThat(SyllableAutoSegmenter.segment(contour, emptyList())).isNull()
        assertThat(SyllableAutoSegmenter.segment(contour, listOf(6))).isNull()
    }

    @Test
    fun `a flat voiced region falls back to an even split`() {
        val frames = List(30) { true to 0.3f }
        val contour = contourOf(frames)

        val boundaries = SyllableAutoSegmenter.segment(contour, listOf(1, 1, 1))

        assertThat(boundaries).hasSize(3)
        assertThat(boundaries!!.map { it.startMs to it.endMs })
            .containsExactly(0 to 100, 100 to 200, 200 to 300)
            .inOrder()
    }

    @Test
    fun `more voiced regions than syllables merge across the shallowest valley`() {
        val frames = buildList {
            repeat(5) { add(true to 0.3f) }
            repeat(5) { add(false to 0.001f) }
            repeat(5) { add(true to 0.3f) }
            repeat(5) { add(false to 0.05f) }
            repeat(5) { add(true to 0.3f) }
        }
        val contour = contourOf(frames)

        val boundaries = SyllableAutoSegmenter.segment(contour, listOf(1, 2))

        assertThat(boundaries).hasSize(2)
        assertThat(boundaries!![0].startMs).isEqualTo(0)
        assertThat(boundaries[1].startMs).isEqualTo(100)
    }

    @Test
    fun `segmentation is deterministic`() {
        val contour = extract(
            dip(
                pcm = tone(startHz = 150f, endHz = 150f, durationMs = 600),
                centersMs = listOf(200, 400),
                widthMs = 60,
                factor = 0.1f,
            ),
        )

        val first = SyllableAutoSegmenter.segment(contour, listOf(1, 1, 1))
        val second = SyllableAutoSegmenter.segment(contour, listOf(1, 1, 1))

        assertThat(first).isEqualTo(second)
    }

    private companion object {
        const val HOP_MS = 10
    }
}
