package com.learnhuayu.core.assessment

class PcmAudio(
    val samples: FloatArray,
    val sampleRateHz: Int,
) {
    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
    }

    val durationMs: Int
        get() = (samples.size.toLong() * 1000L / sampleRateHz).toInt()

    fun isFinite(): Boolean = samples.all { it.isFinite() }
}
