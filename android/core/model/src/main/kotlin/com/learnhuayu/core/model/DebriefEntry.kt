package com.learnhuayu.core.model

import java.time.Instant

data class DebriefEntry(
    val id: String,
    val missionId: String,
    val pinyin: String,
    val kind: DebriefKind,
    val note: String? = null,
    val createdAt: Instant,
)

enum class DebriefKind {
    UNFAMILIAR_WORD,
    TONE_BREAKDOWN,
}
