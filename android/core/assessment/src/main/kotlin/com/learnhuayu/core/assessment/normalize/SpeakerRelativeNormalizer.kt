package com.learnhuayu.core.assessment.normalize

import com.learnhuayu.core.assessment.dsp.FeatureContour
import kotlin.math.log2

data class NormalizedFrame(
    val index: Int,
    val timeMs: Int,
    val level: Float?,
    val voiced: Boolean,
    val loudnessRatio: Float,
    val clarity: Float,
)

class NormalizedContour(
    val frames: List<NormalizedFrame>,
    val sampleRateHz: Int,
    val hopMs: Int,
    val voicedRatio: Float,
    val pitchRangeSemitones: Float,
) {
    fun framesIn(range: IntRange): List<NormalizedFrame> {
        if (range.isEmpty()) return emptyList()
        val from = range.first.coerceAtLeast(0)
        val to = range.last.coerceAtMost(frames.size - 1)
        return if (from > to) emptyList() else frames.subList(from, to + 1)
    }
}

class SpeakerRelativeNormalizer(
    private val lowerPercentile: Float = 0.10f,
    private val upperPercentile: Float = 0.90f,
    private val minimumVoicedFrames: Int = 6,
    private val minimumPitchRangeSemitones: Float = 1.0f,
    private val loudnessFloor: Float = 1e-6f,
) {

    fun normalize(contour: FeatureContour): NormalizedContour? {
        val voicedFrames = contour.frames.filter { it.voiced && it.f0Hz != null && it.f0Hz > 0f }
        if (voicedFrames.size < minimumVoicedFrames) return null

        val semitones = voicedFrames.map { hzToSemitones(it.f0Hz!!) }.sorted()
        val low = percentile(semitones, lowerPercentile)
        val high = percentile(semitones, upperPercentile)
        val range = high - low
        if (range < minimumPitchRangeSemitones) return null

        val voicedLoudness = contour.frames.filter { it.voiced && it.rms > 0f }.map { it.rms }.sorted()
        val referenceLoudness = if (voicedLoudness.isEmpty()) {
            loudnessFloor
        } else {
            percentile(voicedLoudness, 0.5f).coerceAtLeast(loudnessFloor)
        }

        val frames = contour.frames.map { frame ->
            val level = if (frame.voiced && frame.f0Hz != null && frame.f0Hz > 0f) {
                ((hzToSemitones(frame.f0Hz) - low) / range).coerceIn(0f, 1f)
            } else {
                null
            }
            NormalizedFrame(
                index = frame.index,
                timeMs = frame.timeMs,
                level = level,
                voiced = frame.voiced,
                loudnessRatio = frame.rms / referenceLoudness,
                clarity = frame.clarity,
            )
        }

        return NormalizedContour(
            frames = frames,
            sampleRateHz = contour.sampleRateHz,
            hopMs = contour.hopMs,
            voicedRatio = contour.voicedRatio,
            pitchRangeSemitones = range,
        )
    }
}

fun hzToSemitones(hz: Float): Float = (12.0 * log2(hz / 55.0)).toFloat()

private fun percentile(sorted: List<Float>, fraction: Float): Float {
    if (sorted.isEmpty()) return 0f
    if (sorted.size == 1) return sorted[0]
    val position = fraction.coerceIn(0f, 1f) * (sorted.size - 1)
    val lower = position.toInt()
    val upper = (lower + 1).coerceAtMost(sorted.size - 1)
    val weight = position - lower
    return sorted[lower] * (1f - weight) + sorted[upper] * weight
}
