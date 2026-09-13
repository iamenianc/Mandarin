package com.learnhuayu.core.audio.wav

data class WavFormat(
    val sampleRateHz: Int,
    val channelCount: Int = 1,
    val bitsPerSample: Int = 16,
) {
    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(channelCount > 0) { "channelCount must be positive" }
        require(bitsPerSample > 0 && bitsPerSample % 8 == 0) {
            "bitsPerSample must be a positive multiple of 8"
        }
    }

    val blockAlign: Int = channelCount * (bitsPerSample / 8)
    val byteRate: Int = sampleRateHz * blockAlign

    companion object {
        const val FORMAT_PCM = 1
    }
}

class WavFormatException(message: String) : IllegalArgumentException(message)
