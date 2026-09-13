package com.learnhuayu.core.assessment.dsp

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.assessment.SyntheticAudio
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class AutocorrelationF0EstimatorTest {

    private val estimator = AutocorrelationF0Estimator()

    private fun frame(frequencyHz: Float, sampleCount: Int = 1024, amplitude: Float = 0.5f): FloatArray = FloatArray(sampleCount) { index ->
        val t = index.toDouble() / SyntheticAudio.SAMPLE_RATE
        (amplitude * sin(2.0 * PI * frequencyHz * t)).toFloat()
    }

    @Test
    fun `tracks a pure tone within tolerance`() {
        val result = estimator.estimate(frame(200f), SyntheticAudio.SAMPLE_RATE)
        assertThat(result.f0Hz).isNotNull()
        assertThat(abs(result.f0Hz!! - 200f)).isLessThan(10f)
        assertThat(result.clarity).isGreaterThan(0.4f)
    }

    @Test
    fun `silence is unvoiced`() {
        val result = estimator.estimate(FloatArray(1024), SyntheticAudio.SAMPLE_RATE)
        assertThat(result.f0Hz).isNull()
    }

    @Test
    fun `low amplitude noise is unvoiced`() {
        val random = java.util.Random(7L)
        val frame = FloatArray(1024) { ((random.nextDouble() * 2.0 - 1.0) * 0.001).toFloat() }
        val result = estimator.estimate(frame, SyntheticAudio.SAMPLE_RATE)
        assertThat(result.f0Hz).isNull()
    }
}
