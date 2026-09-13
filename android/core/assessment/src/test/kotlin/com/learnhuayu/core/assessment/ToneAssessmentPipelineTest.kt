package com.learnhuayu.core.assessment

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.assessment.evidence.ReferenceFeaturePrecomputer
import com.learnhuayu.core.assessment.evidence.ReferenceToneFeatures
import com.learnhuayu.core.assessment.segment.SyllableBoundary
import com.learnhuayu.core.assessment.tone.ContourDirection
import com.learnhuayu.core.assessment.tone.ToneNumber
import org.junit.Test

class ToneAssessmentPipelineTest {

    private val pipeline = ToneAssessmentPipeline()
    private val precomputer = ReferenceFeaturePrecomputer()

    private val boundaries = listOf(
        SyllableBoundary(startMs = 0, endMs = 300, expectedTone = ToneNumber.TONE_1),
        SyllableBoundary(startMs = 300, endMs = 600, expectedTone = ToneNumber.TONE_2),
        SyllableBoundary(startMs = 600, endMs = 900, expectedTone = ToneNumber.TONE_4),
        SyllableBoundary(startMs = 900, endMs = 1200, expectedTone = ToneNumber.TONE_3),
    )

    private fun phrase(): PcmAudio = SyntheticAudio.contour(
        listOf(
            SyntheticAudio.ContourSegment(startHz = 150f, endHz = 150f, durationMs = 300),
            SyntheticAudio.ContourSegment(startHz = 130f, endHz = 190f, durationMs = 300),
            SyntheticAudio.ContourSegment(startHz = 190f, endHz = 120f, durationMs = 300),
            SyntheticAudio.ContourSegment(startHz = 170f, endHz = 120f, durationMs = 150),
            SyntheticAudio.ContourSegment(startHz = 120f, endHz = 180f, durationMs = 150),
        ),
    )

    private fun reference(): ReferenceToneFeatures = precomputer.precompute(phrase(), boundaries)!!

    @Test
    fun `synthetic tone contours classify to their expected tones`() {
        val evidence = pipeline.assess(phrase(), reference())

        assertThat(evidence).isNotNull()
        assertThat(evidence!!.syllables).hasSize(4)
        assertThat(evidence.syllables.map { it.tone })
            .containsExactly(
                ToneNumber.TONE_1,
                ToneNumber.TONE_2,
                ToneNumber.TONE_4,
                ToneNumber.TONE_3,
            )
            .inOrder()
        assertThat(evidence.syllables.all { it.match }).isTrue()
        assertThat(evidence.syllables.map { it.contourDirection })
            .containsExactly(
                ContourDirection.LEVEL,
                ContourDirection.RISING,
                ContourDirection.FALLING,
                ContourDirection.DIP,
            )
            .inOrder()
    }

    @Test
    fun `a monotone attempt yields no evidence`() {
        val monotone = SyntheticAudio.contour(
            listOf(SyntheticAudio.ContourSegment(startHz = 150f, endHz = 150f, durationMs = 1200)),
        )
        assertThat(pipeline.assess(monotone, reference())).isNull()
    }

    @Test
    fun `a monotone reference yields no evidence`() {
        val monotone = SyntheticAudio.contour(
            listOf(SyntheticAudio.ContourSegment(startHz = 150f, endHz = 150f, durationMs = 1200)),
        )
        val monotoneReference = precomputer.precompute(monotone, boundaries)!!
        assertThat(pipeline.assess(phrase(), monotoneReference)).isNull()
    }

    @Test
    fun `a noisy attempt yields no evidence`() {
        val noisy = SyntheticAudio.noise(durationMs = 1200, amplitude = 0.002f)
        assertThat(pipeline.assess(noisy, reference())).isNull()
    }

    @Test
    fun `a silent attempt yields no evidence`() {
        val silent = PcmAudio(FloatArray(SyntheticAudio.SAMPLE_RATE * 1200 / 1000), SyntheticAudio.SAMPLE_RATE)
        assertThat(pipeline.assess(silent, reference())).isNull()
    }

    @Test
    fun `a too-short attempt yields no evidence`() {
        val tooShort = PcmAudio(FloatArray(100), SyntheticAudio.SAMPLE_RATE)
        assertThat(pipeline.assess(tooShort, reference())).isNull()
    }
}
