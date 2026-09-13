package com.learnhuayu.core.assessment.dsp

data class PitchEstimate(
    val f0Hz: Float?,
    val clarity: Float,
) {
    companion object {
        val UNVOICED = PitchEstimate(f0Hz = null, clarity = 0f)
    }
}

fun interface F0Estimator {
    fun estimate(frame: FloatArray, sampleRateHz: Int): PitchEstimate
}
