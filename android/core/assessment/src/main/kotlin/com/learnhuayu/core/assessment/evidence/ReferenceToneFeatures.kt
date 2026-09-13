package com.learnhuayu.core.assessment.evidence

import com.learnhuayu.core.assessment.dsp.FeatureContour
import com.learnhuayu.core.assessment.dsp.FrameFeature
import com.learnhuayu.core.assessment.segment.SyllableBoundary
import kotlinx.serialization.Serializable

@Serializable
data class ReferenceFrame(
    val timeMs: Int,
    val f0Hz: Float? = null,
    val clarity: Float = 0f,
    val voiced: Boolean,
    val rms: Float,
)

@Serializable
data class ReferenceToneFeatures(
    val version: Int = SCHEMA_VERSION,
    val sampleRateHz: Int,
    val frameLengthMs: Int,
    val hopMs: Int,
    val frames: List<ReferenceFrame>,
    val syllables: List<SyllableBoundary>,
) {

    fun toContour(): FeatureContour = FeatureContour(
        frames = frames.mapIndexed { index, frame ->
            FrameFeature(
                index = index,
                timeMs = frame.timeMs,
                f0Hz = frame.f0Hz,
                clarity = frame.clarity,
                voiced = frame.voiced,
                rms = frame.rms,
            )
        },
        sampleRateHz = sampleRateHz,
        frameLengthMs = frameLengthMs,
        hopMs = hopMs,
    )

    companion object {
        const val SCHEMA_VERSION = 1
    }
}
