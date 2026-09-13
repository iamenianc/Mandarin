package com.learnhuayu.core.assessment.segment

import com.learnhuayu.core.assessment.dsp.FeatureContour
import com.learnhuayu.core.assessment.tone.ToneNumber

/**
 * Derives syllable boundaries for a reference clip from its measured contour and the
 * expected tone sequence (ADR 0014). Kokoro reference clips carry no timestamps (ADR 0006),
 * so the boundaries are recovered on device from the clip's own energy contour.
 *
 * The segmenter is deterministic: one contour and tone sequence always produce the same
 * boundaries. Voiced runs are used as a first estimate. When there are enough runs, the
 * deepest energy valleys between them are kept as split points and the shallowest are
 * merged; when there are fewer runs than syllables, the deepest valleys inside the runs
 * divide them further. When neither source yields enough boundaries, the voiced span is
 * split evenly. A contour with no voiced frames, an empty tone sequence, or an out-of-range
 * tone number yields `null`.
 */
object SyllableAutoSegmenter {

    private const val MINIMUM_SPLIT_SEPARATION_FRAMES = 2
    private const val MINIMUM_VALLEY_DEPTH = 0.05f

    fun segment(contour: FeatureContour, expectedTones: List<Int>): List<SyllableBoundary>? {
        if (expectedTones.isEmpty() || contour.frames.isEmpty()) return null
        if (expectedTones.any { !ToneNumber.isValid(it) }) return null

        val voicedRegions = voicedRegions(contour)
        if (voicedRegions.isEmpty()) return null

        val segments = when {
            voicedRegions.size == expectedTones.size -> voicedRegions

            voicedRegions.size > expectedTones.size ->
                mergeVoicedRegions(contour, voicedRegions, expectedTones.size)

            else -> splitVoicedRegions(contour, voicedRegions, expectedTones.size)
                ?: evenSplit(voicedRegions.first().first, voicedRegions.last().last, expectedTones.size)
                ?: return null
        }

        return toBoundaries(contour, segments, expectedTones)
    }

    private fun voicedRegions(contour: FeatureContour): List<IntRange> {
        val regions = ArrayList<IntRange>()
        var start = -1
        for (index in contour.frames.indices) {
            if (contour.frames[index].voiced) {
                if (start < 0) start = index
            } else if (start >= 0) {
                regions += start..(index - 1)
                start = -1
            }
        }
        if (start >= 0) regions += start..contour.frames.lastIndex
        return regions
    }

    private fun mergeVoicedRegions(contour: FeatureContour, regions: List<IntRange>, count: Int): List<IntRange> {
        val merged = regions.toMutableList()
        while (merged.size > count) {
            var mergeIndex = -1
            var shallowest = Float.NEGATIVE_INFINITY
            for (index in 0 until merged.size - 1) {
                val valley = gapValley(contour, merged[index].last, merged[index + 1].first)
                if (valley > shallowest) {
                    shallowest = valley
                    mergeIndex = index
                }
            }
            if (mergeIndex < 0) break
            merged[mergeIndex] = merged[mergeIndex].first..merged[mergeIndex + 1].last
            merged.removeAt(mergeIndex + 1)
        }
        return merged
    }

    private fun gapValley(contour: FeatureContour, leftLast: Int, rightFirst: Int): Float {
        if (rightFirst <= leftLast + 1) return 0f
        var minimum = Float.POSITIVE_INFINITY
        for (index in (leftLast + 1) until rightFirst) {
            minimum = minOf(minimum, contour.frames[index].rms)
        }
        return if (minimum.isFinite()) minimum else 0f
    }

    private fun splitVoicedRegions(contour: FeatureContour, regions: List<IntRange>, count: Int): List<IntRange>? {
        val segments = regions.toMutableList()
        while (segments.size < count) {
            val valley = deepestValley(contour, segments) ?: return null
            val region = segments[valley.regionIndex]
            segments[valley.regionIndex] = region.first..valley.frame
            segments.add(valley.regionIndex + 1, (valley.frame + 1)..region.last)
        }
        return segments
    }

    private fun deepestValley(contour: FeatureContour, segments: List<IntRange>): Valley? {
        var best: Valley? = null
        for (regionIndex in segments.indices) {
            val region = segments[regionIndex]
            val from = region.first + MINIMUM_SPLIT_SEPARATION_FRAMES
            val to = region.last - MINIMUM_SPLIT_SEPARATION_FRAMES
            if (from > to) continue
            for (frame in from..to) {
                val depth = valleyDepth(contour, frame)
                if (depth < MINIMUM_VALLEY_DEPTH) continue
                val candidate = Valley(regionIndex, frame, depth)
                if (best == null || candidate.isPreferredOver(best)) best = candidate
            }
        }
        return best
    }

    private fun valleyDepth(contour: FeatureContour, frame: Int): Float {
        if (frame <= 0 || frame >= contour.frames.lastIndex) return 0f
        val current = contour.frames[frame].rms
        return minOf(contour.frames[frame - 1].rms, contour.frames[frame + 1].rms) - current
    }

    private fun evenSplit(firstFrame: Int, lastFrame: Int, count: Int): List<IntRange>? {
        val total = lastFrame - firstFrame + 1
        if (count <= 0 || total < count) return null
        return (0 until count).map { index ->
            val start = firstFrame + index * total / count
            val end = firstFrame + (index + 1) * total / count - 1
            start..end
        }
    }

    private fun toBoundaries(
        contour: FeatureContour,
        segments: List<IntRange>,
        expectedTones: List<Int>,
    ): List<SyllableBoundary>? {
        if (segments.size != expectedTones.size) return null
        return segments.mapIndexed { index, segment ->
            val startMs = contour.timeMsAt(segment.first)
            val rawEndMs = contour.timeMsAt(segment.last) + contour.hopMs
            val endMs = minOf(rawEndMs, contour.durationMs)
            if (endMs <= startMs) return null
            SyllableBoundary(startMs = startMs, endMs = endMs, expectedTone = expectedTones[index])
        }
    }

    private data class Valley(
        val regionIndex: Int,
        val frame: Int,
        val depth: Float,
    ) {
        fun isPreferredOver(other: Valley): Boolean = when {
            depth != other.depth -> depth > other.depth
            frame != other.frame -> frame < other.frame
            else -> regionIndex < other.regionIndex
        }
    }
}
