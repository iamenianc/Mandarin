package com.learnhuayu.core.assessment.evidence

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.assessment.tone.ContourDirection
import kotlinx.serialization.json.Json
import org.junit.Test

class ToneEvidenceSerializationTest {

    private val json = Json { encodeDefaults = true }

    private val evidence = ToneEvidence(
        syllables = listOf(
            SyllableToneObservation(
                index = 0,
                tone = 3,
                expectedTone = 3,
                match = true,
                contourDirection = ContourDirection.DIP,
                voicingRatio = 0.9f,
                loudnessRatio = 1.0f,
            ),
            SyllableToneObservation(
                index = 1,
                tone = null,
                expectedTone = 2,
                match = false,
                contourDirection = ContourDirection.UNKNOWN,
                voicingRatio = 0.2f,
                loudnessRatio = 0.5f,
            ),
        ),
    )

    @Test
    fun `evidence round-trips through serialization`() {
        val encoded = json.encodeToString(ToneEvidence.serializer(), evidence)
        val decoded = json.decodeFromString(ToneEvidence.serializer(), encoded)
        assertThat(decoded).isEqualTo(evidence)
    }

    @Test
    fun `evidence carries named per-syllable observations and no raw contours`() {
        val encoded = json.encodeToString(ToneEvidence.serializer(), evidence)

        assertThat(encoded).contains("syllables")
        assertThat(encoded).contains("expectedTone")
        assertThat(encoded).contains("contourDirection")
        assertThat(encoded).contains("voicingRatio")
        assertThat(encoded).doesNotContain("f0Hz")
        assertThat(encoded).doesNotContain("frames")
        assertThat(encoded).doesNotContain("rms")
    }
}
