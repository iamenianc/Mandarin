package com.learnhuayu.core.assessment.evidence

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.assessment.PcmAudio
import com.learnhuayu.core.assessment.SyntheticAudio
import com.learnhuayu.core.assessment.segment.SyllableBoundary
import com.learnhuayu.core.assessment.tone.ToneNumber
import org.junit.Test

class ReferenceFeaturePrecomputerTest {

    private val precomputer = ReferenceFeaturePrecomputer()

    private val boundaries = listOf(
        SyllableBoundary(startMs = 0, endMs = 300, expectedTone = ToneNumber.TONE_2),
        SyllableBoundary(startMs = 300, endMs = 600, expectedTone = ToneNumber.TONE_4),
    )

    private fun clip(): PcmAudio = SyntheticAudio.contour(
        listOf(
            SyntheticAudio.ContourSegment(startHz = 130f, endHz = 190f, durationMs = 300),
            SyntheticAudio.ContourSegment(startHz = 190f, endHz = 120f, durationMs = 300),
        ),
    )

    @Test
    fun `precomputes reference features for caller-provided boundaries`() {
        val features = precomputer.precompute(clip(), boundaries)

        assertThat(features).isNotNull()
        assertThat(features!!.sampleRateHz).isEqualTo(SyntheticAudio.SAMPLE_RATE)
        assertThat(features.hopMs).isGreaterThan(0)
        assertThat(features.frames).isNotEmpty()
        assertThat(features.syllables).isEqualTo(boundaries)
        assertThat(features.version).isEqualTo(ReferenceToneFeatures.SCHEMA_VERSION)
    }

    @Test
    fun `no boundaries yields no reference features`() {
        assertThat(precomputer.precompute(clip(), emptyList())).isNull()
    }

    @Test
    fun `boundaries beyond the clip yield no reference features`() {
        val invalid = listOf(SyllableBoundary(startMs = 0, endMs = 5000, expectedTone = ToneNumber.TONE_1))
        assertThat(precomputer.precompute(clip(), invalid)).isNull()
    }

    @Test
    fun `reference features round-trip through the codec`() {
        val features = precomputer.precompute(clip(), boundaries)!!

        val encoded = ReferenceFeatureCodec.encode(features)
        val decoded = ReferenceFeatureCodec.decode(encoded)

        assertThat(decoded).isEqualTo(features)
    }

    @Test
    fun `malformed reference JSON decodes to null`() {
        assertThat(ReferenceFeatureCodec.decode("not json")).isNull()
    }
}
