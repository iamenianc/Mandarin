package com.learnhuayu.core.assessment.evidence

import com.learnhuayu.core.assessment.tone.ContourDirection
import kotlinx.serialization.Serializable

@Serializable
data class ToneEvidence(
    val version: Int = SCHEMA_VERSION,
    val syllables: List<SyllableToneObservation>,
) {
    companion object {
        const val SCHEMA_VERSION = 1
        const val PAYLOAD_KEY = "acousticEvidence"
    }
}

@Serializable
data class SyllableToneObservation(
    val index: Int,
    val tone: Int?,
    val expectedTone: Int,
    val match: Boolean,
    val contourDirection: ContourDirection,
    val voicingRatio: Float,
    val loudnessRatio: Float,
)
