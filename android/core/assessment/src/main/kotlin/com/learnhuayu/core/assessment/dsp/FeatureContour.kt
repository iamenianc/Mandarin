package com.learnhuayu.core.assessment.dsp

data class FrameFeature(
    val index: Int,
    val timeMs: Int,
    val f0Hz: Float?,
    val clarity: Float,
    val voiced: Boolean,
    val rms: Float,
)

class FeatureContour(
    val frames: List<FrameFeature>,
    val sampleRateHz: Int,
    val frameLengthMs: Int,
    val hopMs: Int,
) {
    val durationMs: Int
        get() = frames.lastOrNull()?.let { it.timeMs + frameLengthMs } ?: 0

    val voicedFrameCount: Int
        get() = frames.count { it.voiced }

    val voicedRatio: Float
        get() = if (frames.isEmpty()) 0f else voicedFrameCount.toFloat() / frames.size

    fun framesIn(range: IntRange): List<FrameFeature> {
        if (range.isEmpty()) return emptyList()
        val from = range.first.coerceAtLeast(0)
        val to = range.last.coerceAtMost(frames.size - 1)
        return if (from > to) emptyList() else frames.subList(from, to + 1)
    }

    fun frameIndexAt(timeMs: Int): Int = timeMs / hopMs

    fun timeMsAt(frameIndex: Int): Int = frameIndex * hopMs
}
