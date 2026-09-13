package com.learnhuayu.core.assessment

import kotlin.math.PI
import kotlin.math.sin

object SyntheticAudio {
    const val SAMPLE_RATE = 16_000

    data class ContourSegment(
        val startHz: Float,
        val endHz: Float,
        val durationMs: Int,
    )

    fun contour(
        segments: List<ContourSegment>,
        amplitude: Float = 0.4f,
    ): PcmAudio {
        val samples = ArrayList<Float>()
        var phase = 0.0
        for (segment in segments) {
            val count = SAMPLE_RATE * segment.durationMs / 1000
            for (index in 0 until count) {
                val fraction = if (count <= 1) 1.0 else index.toDouble() / (count - 1)
                val frequency = segment.startHz + (segment.endHz - segment.startHz) * fraction
                phase += 2.0 * PI * frequency / SAMPLE_RATE
                samples += (amplitude * sin(phase)).toFloat()
            }
        }
        return PcmAudio(samples.toFloatArray(), SAMPLE_RATE)
    }

    fun noise(
        durationMs: Int,
        amplitude: Float,
        seed: Long = 1234L,
    ): PcmAudio {
        val random = java.util.Random(seed)
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = FloatArray(count) { ((random.nextDouble() * 2.0 - 1.0) * amplitude).toFloat() }
        return PcmAudio(samples, SAMPLE_RATE)
    }
}
