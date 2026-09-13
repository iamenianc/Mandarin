package com.learnhuayu.core.model

import java.time.Instant

data class RaymondMessage(
    val id: String,
    val role: RaymondRole,
    val text: String,
    val audioRef: String? = null,
    val createdAt: Instant,
)

enum class RaymondRole {
    USER,
    RAYMOND,
}
