package com.learnhuayu.core.assessment.tone

import kotlin.math.abs

data class SyllableStats(
    val onsetLevel: Float?,
    val offsetLevel: Float?,
    val minimumLevel: Float?,
    val minimumPosition: Float,
    val voicedFrames: Int,
    val voicingRatio: Float,
    val loudnessRatio: Float,
    val durationRatio: Float,
)

data class ToneClassification(
    val tone: Int?,
    val direction: ContourDirection,
)

class ToneClassifier(
    private val directionDelta: Float = 0.18f,
    private val dipFallDelta: Float = 0.15f,
    private val dipRiseDelta: Float = 0.08f,
    private val minimumFramesForDirection: Int = 3,
    private val neutralDurationRatio: Float = 0.75f,
    private val neutralLoudnessRatio: Float = 0.85f,
    private val strongDirectionDelta: Float = 0.35f,
) {

    fun classify(stats: SyllableStats): ToneClassification {
        val onset = stats.onsetLevel
        val offset = stats.offsetLevel
        val minimum = stats.minimumLevel
        if (onset == null || offset == null || minimum == null) {
            return ToneClassification(tone = null, direction = ContourDirection.UNKNOWN)
        }
        if (stats.voicedFrames < minimumFramesForDirection) {
            return ToneClassification(tone = null, direction = ContourDirection.UNKNOWN)
        }

        val fall = onset - minimum
        val rise = offset - minimum
        val isDip = fall >= dipFallDelta &&
            rise >= dipRiseDelta &&
            stats.minimumPosition in DIP_POSITION_RANGE

        val delta = offset - onset
        val direction = when {
            isDip -> ContourDirection.DIP
            delta >= directionDelta -> ContourDirection.RISING
            delta <= -directionDelta -> ContourDirection.FALLING
            else -> ContourDirection.LEVEL
        }

        val weak = stats.loudnessRatio <= neutralLoudnessRatio &&
            stats.durationRatio <= neutralDurationRatio

        val tone = when {
            direction == ContourDirection.DIP -> ToneNumber.TONE_3
            weak && abs(delta) < strongDirectionDelta -> ToneNumber.NEUTRAL
            direction == ContourDirection.RISING -> ToneNumber.TONE_2
            direction == ContourDirection.FALLING -> ToneNumber.TONE_4
            else -> ToneNumber.TONE_1
        }
        return ToneClassification(tone = tone, direction = direction)
    }

    private companion object {
        val DIP_POSITION_RANGE = 0.1f..0.95f
    }
}
