package com.learnhuayu.core.assessment.dsp

import kotlin.math.abs
import kotlin.math.sqrt

class AutocorrelationF0Estimator(
    private val minimumF0Hz: Float = 70f,
    private val maximumF0Hz: Float = 450f,
    private val clarityThreshold: Float = 0.4f,
) : F0Estimator {

    override fun estimate(frame: FloatArray, sampleRateHz: Int): PitchEstimate {
        val minimumLag = (sampleRateHz / maximumF0Hz).toInt().coerceAtLeast(1)
        val maximumLag = (sampleRateHz / minimumF0Hz).toInt().coerceAtMost(frame.size - 1)
        if (maximumLag <= minimumLag) return PitchEstimate.UNVOICED

        var mean = 0.0
        for (sample in frame) {
            mean += sample
        }
        mean /= frame.size

        val centered = DoubleArray(frame.size)
        for (index in frame.indices) {
            centered[index] = frame[index] - mean
        }

        val prefixSquares = DoubleArray(frame.size + 1)
        for (index in centered.indices) {
            prefixSquares[index + 1] = prefixSquares[index] + centered[index] * centered[index]
        }
        val totalEnergy = prefixSquares[frame.size]
        if (totalEnergy <= MINIMUM_ENERGY) return PitchEstimate.UNVOICED

        val correlations = FloatArray(maximumLag + 1)
        var bestLag = -1
        var bestCorrelation = Float.NEGATIVE_INFINITY
        for (lag in minimumLag..maximumLag) {
            val overlap = frame.size - lag
            val energyA = prefixSquares[overlap]
            val energyB = totalEnergy - prefixSquares[lag]
            if (energyA <= MINIMUM_ENERGY || energyB <= MINIMUM_ENERGY) continue
            var dot = 0.0
            for (index in 0 until overlap) {
                dot += centered[index] * centered[index + lag]
            }
            val correlation = (dot / sqrt(energyA * energyB)).toFloat()
            correlations[lag] = correlation
            if (correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestLag = lag
            }
        }

        if (bestLag < 0 || bestCorrelation < clarityThreshold) return PitchEstimate.UNVOICED

        val octaveCorrectedLag = correctOctave(correlations, bestLag, minimumLag)
        val refinedLag = parabolicPeakLag(correlations, octaveCorrectedLag, minimumLag, maximumLag)
            ?: octaveCorrectedLag.toFloat()
        if (refinedLag <= 0f) return PitchEstimate.UNVOICED

        val f0Hz = sampleRateHz / refinedLag
        if (!f0Hz.isFinite() || f0Hz < minimumF0Hz || f0Hz > maximumF0Hz) return PitchEstimate.UNVOICED
        return PitchEstimate(f0Hz = f0Hz, clarity = bestCorrelation)
    }

    private fun correctOctave(correlations: FloatArray, bestLag: Int, minimumLag: Int): Int {
        var lag = bestLag
        while (lag >= 2 * minimumLag) {
            val half = lag / 2
            val from = (half - 1).coerceAtLeast(minimumLag)
            val to = (half + 1).coerceAtMost(correlations.size - 1)
            var localLag = -1
            var localValue = Float.NEGATIVE_INFINITY
            for (candidate in from..to) {
                if (correlations[candidate] > localValue) {
                    localValue = correlations[candidate]
                    localLag = candidate
                }
            }
            val reference = correlations[bestLag]
            if (localLag < 0 || localValue < OCTAVE_CORRECTION_FACTOR * reference) break
            lag = localLag
        }
        return lag
    }

    private fun parabolicPeakLag(
        correlations: FloatArray,
        lag: Int,
        minimumLag: Int,
        maximumLag: Int,
    ): Float? {
        if (lag <= minimumLag || lag >= maximumLag) return null
        val left = correlations[lag - 1]
        val center = correlations[lag]
        val right = correlations[lag + 1]
        val denominator = left - 2f * center + right
        if (abs(denominator) < MINIMUM_CURVATURE) return null
        val offset = 0.5f * (left - right) / denominator
        return lag + offset.coerceIn(-1f, 1f)
    }

    private companion object {
        const val MINIMUM_ENERGY = 1e-9
        const val MINIMUM_CURVATURE = 1e-6f
        const val OCTAVE_CORRECTION_FACTOR = 0.85f
    }
}
