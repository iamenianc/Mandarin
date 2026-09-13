package com.learnhuayu.core.audio.pcm

object AudioSpec {
    const val SAMPLE_RATE_HZ = 24_000
    const val CHANNEL_COUNT = 1
    const val BITS_PER_SAMPLE = 16
    const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8
    const val BYTES_PER_FRAME = CHANNEL_COUNT * BYTES_PER_SAMPLE
}
