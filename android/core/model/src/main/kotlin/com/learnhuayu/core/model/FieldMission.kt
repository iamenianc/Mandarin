package com.learnhuayu.core.model

import java.time.LocalDate

data class FieldMission(
    val id: String,
    val date: LocalDate,
    val theme: String,
    val scriptTurns: List<ScriptTurn>,
    val source: ContentSource,
    val localPersonaIds: List<String>,
)

data class ScriptTurn(
    val pinyin: String,
    val meaning: String,
    val targetTones: List<Int>,
)
