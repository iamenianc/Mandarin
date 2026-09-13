package com.learnhuayu.core.audio.pcm

class PcmAudio(
    val samples: ShortArray,
    val sampleRateHz: Int,
) {
    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
    }

    val sampleCount: Int
        get() = samples.size

    val durationMs: Long
        get() = samples.size * 1000L / sampleRateHz

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PcmAudio) return false
        return sampleRateHz == other.sampleRateHz && samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int = 31 * samples.contentHashCode() + sampleRateHz

    override fun toString(): String = "PcmAudio(sampleCount=$sampleCount, sampleRateHz=$sampleRateHz)"
}
