package com.learnhuayu.core.model

import java.time.Instant

data class Progress(
    val contentItemId: String,
    val timesPracticed: Int,
    val lastPracticedAt: Instant? = null,
    val feedbackThemes: List<String> = emptyList(),
)
