package com.learnhuayu.core.assessment

data class FrameConfig(
    val frameLengthMs: Int = 40,
    val hopMs: Int = 10,
) {
    init {
        require(frameLengthMs >= hopMs) { "frameLengthMs must be at least hopMs" }
        require(hopMs > 0) { "hopMs must be positive" }
    }

    fun frameLengthSamples(sampleRateHz: Int): Int = sampleRateHz * frameLengthMs / 1000

    fun hopSamples(sampleRateHz: Int): Int = sampleRateHz * hopMs / 1000
}
