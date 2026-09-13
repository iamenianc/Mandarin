package com.learnhuayu.core.model

import java.time.Instant

data class Attempt(
    val id: String,
    val contentItemId: String,
    val recordedAt: Instant,
    val userAudioRef: String,
    val transcript: String? = null,
    val feedbackText: String? = null,
)
