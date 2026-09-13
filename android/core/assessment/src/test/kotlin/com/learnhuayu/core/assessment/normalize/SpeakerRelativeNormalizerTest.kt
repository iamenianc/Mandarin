package com.learnhuayu.core.assessment.normalize

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.assessment.dsp.FeatureContour
import com.learnhuayu.core.assessment.dsp.FrameFeature
import org.junit.Test
import kotlin.math.abs

class SpeakerRelativeNormalizerTest {

    private val normalizer = SpeakerRelativeNormalizer()

    private fun contour(
        f0Provider: (Int) -> Float,
        rmsProvider: (Int) -> Float = { 0.3f },
        size: Int = 40,
    ): FeatureContour = FeatureContour(
        frames = List(size) { index ->
            FrameFeature(
                index = index,
                timeMs = index * 10,
                f0Hz = f0Provider(index),
                clarity = 0.9f,
                voiced = true,
                rms = rmsProvider(index),
            )
        },
        sampleRateHz = 16_000,
        frameLengthMs = 40,
        hopMs = 10,
    )

    private fun descendingThenRising(index: Int): Float = if (index < 20) 190f - index * 3.5f else 120f + (index - 20) * 3.5f

    @Test
    fun `levels are invariant to recording gain`() {
        val base = contour(
            f0Provider = ::descendingThenRising,
            rmsProvider = { index -> 0.2f + (index % 5) * 0.05f },
        )
        val louder = contour(
            f0Provider = ::descendingThenRising,
            rmsProvider = { index -> (0.2f + (index % 5) * 0.05f) * 4f },
        )

        val baseNormalized = normalizer.normalize(base)
        val louderNormalized = normalizer.normalize(louder)

        assertThat(baseNormalized).isNotNull()
        assertThat(louderNormalized).isNotNull()
        for (index in baseNormalized!!.frames.indices) {
            assertThat(abs(baseNormalized.frames[index].loudnessRatio - louderNormalized!!.frames[index].loudnessRatio))
                .isLessThan(1e-4f)
        }
    }

    @Test
    fun `levels are invariant to pitch register`() {
        val base = contour(f0Provider = ::descendingThenRising)
        val higher = contour(f0Provider = { index -> descendingThenRising(index) * 1.5f })

        val baseNormalized = normalizer.normalize(base)
        val higherNormalized = normalizer.normalize(higher)

        assertThat(baseNormalized).isNotNull()
        assertThat(higherNormalized).isNotNull()
        for (index in baseNormalized!!.frames.indices) {
            val baseLevel = baseNormalized.frames[index].level
            val higherLevel = higherNormalized!!.frames[index].level
            assertThat(baseLevel).isNotNull()
            assertThat(higherLevel).isNotNull()
            assertThat(abs(baseLevel!! - higherLevel!!)).isLessThan(1e-4f)
        }
    }

    @Test
    fun `monotone pitch produces no normalized contour`() {
        val monotone = contour(f0Provider = { 150f })
        assertThat(normalizer.normalize(monotone)).isNull()
    }

    @Test
    fun `too few voiced frames produces no normalized contour`() {
        val sparse = FeatureContour(
            frames = List(40) { index ->
                FrameFeature(
                    index = index,
                    timeMs = index * 10,
                    f0Hz = if (index < 3) 120f + index * 20f else null,
                    clarity = if (index < 3) 0.9f else 0f,
                    voiced = index < 3,
                    rms = 0.3f,
                )
            },
            sampleRateHz = 16_000,
            frameLengthMs = 40,
            hopMs = 10,
        )
        assertThat(normalizer.normalize(sparse)).isNull()
    }
}
