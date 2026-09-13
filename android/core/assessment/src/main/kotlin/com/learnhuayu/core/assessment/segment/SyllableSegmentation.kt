package com.learnhuayu.core.assessment.segment

import com.learnhuayu.core.assessment.dsp.FeatureContour
import com.learnhuayu.core.assessment.tone.ToneNumber
import kotlinx.serialization.Serializable

@Serializable
data class SyllableBoundary(
    val startMs: Int,
    val endMs: Int,
    val expectedTone: Int,
) {
    init {
        require(startMs >= 0) { "startMs must not be negative" }
        require(endMs > startMs) { "endMs must be greater than startMs" }
        require(ToneNumber.isValid(expectedTone)) { "expectedTone must be a tone number from 1 to 5" }
    }
}

data class SyllableSpan(
    val index: Int,
    val boundary: SyllableBoundary,
    val frameRange: IntRange,
)

object SyllableSegmenter {

    fun segment(contour: FeatureContour, boundaries: List<SyllableBoundary>): List<SyllableSpan>? {
        if (boundaries.isEmpty() || contour.frames.isEmpty()) return null
        if (boundaries.last().endMs > contour.durationMs) return null

        val spans = ArrayList<SyllableSpan>(boundaries.size)
        var previousEndMs = 0
        for ((index, boundary) in boundaries.withIndex()) {
            if (boundary.startMs < previousEndMs) return null

            val firstFrame = contour.frameIndexAt(boundary.startMs)
            val lastFrameExclusive = ceilDiv(boundary.endMs, contour.hopMs)
            if (firstFrame >= contour.frames.size) return null

            val lastFrame = (lastFrameExclusive - 1).coerceAtMost(contour.frames.size - 1)
            if (lastFrame < firstFrame) return null

            spans += SyllableSpan(index = index, boundary = boundary, frameRange = firstFrame..lastFrame)
            previousEndMs = boundary.endMs
        }
        return spans
    }

    private fun ceilDiv(value: Int, divisor: Int): Int = (value + divisor - 1) / divisor
}
