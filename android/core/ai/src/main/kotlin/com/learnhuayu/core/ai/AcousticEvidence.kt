package com.learnhuayu.core.ai

import kotlinx.serialization.Serializable

@Serializable
data class AcousticEvidence(
    val phrase: String? = null,
    val syllables: List<AcousticSyllable> = emptyList(),
)

@Serializable
data class AcousticSyllable(
    val pinyin: String,
    val expectedTone: Int,
    val observed: String? = null,
    val direction: String? = null,
    val span: String? = null,
    val onsetLevel: Int? = null,
    val offsetLevel: Int? = null,
    val voicingRatio: Double? = null,
    val voicedMs: Int? = null,
    val loudness: String? = null,
    val note: String? = null,
)
