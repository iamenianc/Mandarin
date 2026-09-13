package com.learnhuayu.core.model

import java.time.Instant

data class ConversationSession(
    val id: String,
    val lessonId: String,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val turnCount: Int = 0,
    val summary: String? = null,
)
