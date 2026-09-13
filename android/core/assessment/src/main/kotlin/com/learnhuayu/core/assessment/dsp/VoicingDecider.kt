package com.learnhuayu.core.assessment.dsp

fun interface VoicingDecider {
    fun isVoiced(frame: FloatArray, rms: Float, estimate: PitchEstimate, sampleRateHz: Int): Boolean
}

class EnergyClarityVoicingDecider(
    private val minimumClarity: Float = 0.45f,
    private val minimumRms: Float = 0.01f,
) : VoicingDecider {

    override fun isVoiced(frame: FloatArray, rms: Float, estimate: PitchEstimate, sampleRateHz: Int): Boolean {
        val f0Hz = estimate.f0Hz ?: return false
        if (!f0Hz.isFinite() || f0Hz <= 0f) return false
        if (estimate.clarity < minimumClarity) return false
        return rms >= minimumRms
    }
}
