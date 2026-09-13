package com.learnhuayu.core.model

data class PracticeSpec(
    val id: String,
    val moduleId: String,
    val title: String,
    val contentItemIds: List<String>,
    val mode: DrillMode = DrillMode.LISTEN_AND_CHOOSE,
)
