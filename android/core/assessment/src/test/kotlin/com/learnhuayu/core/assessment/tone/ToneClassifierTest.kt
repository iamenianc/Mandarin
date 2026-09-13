package com.learnhuayu.core.assessment.tone

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ToneClassifierTest {

    private val classifier = ToneClassifier()

    private fun stats(
        onset: Float,
        offset: Float,
        minimum: Float,
        minimumPosition: Float,
        voicedFrames: Int = 12,
        loudnessRatio: Float = 1.0f,
        durationRatio: Float = 1.0f,
    ) = SyllableStats(
        onsetLevel = onset,
        offsetLevel = offset,
        minimumLevel = minimum,
        minimumPosition = minimumPosition,
        voicedFrames = voicedFrames,
        voicingRatio = 0.9f,
        loudnessRatio = loudnessRatio,
        durationRatio = durationRatio,
    )

    @Test
    fun `flat high contour classifies as tone one`() {
        val result = classifier.classify(stats(onset = 0.7f, offset = 0.7f, minimum = 0.7f, minimumPosition = 0f))
        assertThat(result.direction).isEqualTo(ContourDirection.LEVEL)
        assertThat(result.tone).isEqualTo(ToneNumber.TONE_1)
    }

    @Test
    fun `rising contour classifies as tone two`() {
        val result = classifier.classify(stats(onset = 0.25f, offset = 0.75f, minimum = 0.25f, minimumPosition = 0f))
        assertThat(result.direction).isEqualTo(ContourDirection.RISING)
        assertThat(result.tone).isEqualTo(ToneNumber.TONE_2)
    }

    @Test
    fun `falling contour classifies as tone four`() {
        val result = classifier.classify(stats(onset = 0.85f, offset = 0.3f, minimum = 0.3f, minimumPosition = 1f))
        assertThat(result.direction).isEqualTo(ContourDirection.FALLING)
        assertThat(result.tone).isEqualTo(ToneNumber.TONE_4)
    }

    @Test
    fun `dipping contour classifies as tone three`() {
        val result = classifier.classify(stats(onset = 0.75f, offset = 0.65f, minimum = 0.2f, minimumPosition = 0.5f))
        assertThat(result.direction).isEqualTo(ContourDirection.DIP)
        assertThat(result.tone).isEqualTo(ToneNumber.TONE_3)
    }

    @Test
    fun `short weak level contour classifies as neutral tone`() {
        val result = classifier.classify(
            stats(
                onset = 0.5f,
                offset = 0.55f,
                minimum = 0.5f,
                minimumPosition = 0f,
                loudnessRatio = 0.5f,
                durationRatio = 0.4f,
            ),
        )
        assertThat(result.tone).isEqualTo(ToneNumber.NEUTRAL)
    }

    @Test
    fun `too few voiced frames yields no tone`() {
        val result = classifier.classify(
            stats(onset = 0.25f, offset = 0.75f, minimum = 0.25f, minimumPosition = 0f, voicedFrames = 2),
        )
        assertThat(result.tone).isNull()
        assertThat(result.direction).isEqualTo(ContourDirection.UNKNOWN)
    }

    @Test
    fun `missing levels yields no tone`() {
        val result = classifier.classify(
            SyllableStats(
                onsetLevel = null,
                offsetLevel = null,
                minimumLevel = null,
                minimumPosition = 0f,
                voicedFrames = 0,
                voicingRatio = 0f,
                loudnessRatio = 0f,
                durationRatio = 0f,
            ),
        )
        assertThat(result.tone).isNull()
        assertThat(result.direction).isEqualTo(ContourDirection.UNKNOWN)
    }
}
