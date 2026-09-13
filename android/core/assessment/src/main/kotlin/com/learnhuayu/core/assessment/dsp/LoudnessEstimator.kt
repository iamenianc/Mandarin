package com.learnhuayu.core.assessment.dsp

import kotlin.math.sqrt

fun interface LoudnessEstimator {
    fun rms(frame: FloatArray): Float
}

class RmsLoudnessEstimator : LoudnessEstimator {
    override fun rms(frame: FloatArray): Float {
        if (frame.isEmpty()) return 0f
        var sum = 0.0
        for (sample in frame) {
            sum += sample.toDouble() * sample.toDouble()
        }
        return sqrt(sum / frame.size).toFloat()
    }
}
