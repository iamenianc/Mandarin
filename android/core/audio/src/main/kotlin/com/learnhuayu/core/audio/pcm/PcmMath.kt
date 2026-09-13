package com.learnhuayu.core.audio.pcm

import kotlin.math.abs
import kotlin.math.sqrt

internal object PcmMath {

    fun rms(samples: ShortArray, length: Int = samples.size): Float {
        if (length <= 0) return 0f
        var sum = 0.0
        for (index in 0 until length) {
            val value = samples[index] / 32768.0
            sum += value * value
        }
        return sqrt(sum / length).toFloat().coerceIn(0f, 1f)
    }

    fun peak(samples: ShortArray, length: Int = samples.size): Float {
        var maximum = 0
        for (index in 0 until length) {
            val magnitude = abs(samples[index].toInt())
            if (magnitude > maximum) maximum = magnitude
        }
        return (maximum / 32768f).coerceIn(0f, 1f)
    }

    fun zeroCrossingRate(samples: ShortArray, length: Int = samples.size): Float {
        if (length < 2) return 0f
        var crossings = 0
        for (index in 1 until length) {
            val previousNegative = samples[index - 1] < 0
            val currentNegative = samples[index] < 0
            if (previousNegative != currentNegative) crossings++
        }
        return crossings.toFloat() / (length - 1)
    }
}
