package com.learnhuayu.core.model

data class LessonSpec(
    val id: String,
    val moduleId: String,
    val title: String,
    val level: String,
    val topic: String,
    val contentItemIds: List<String>,
)
