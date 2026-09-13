package com.learnhuayu.core.model

import java.time.Instant

data class MissionSession(
    val id: String,
    val missionId: String,
    val personaId: String,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val turnCount: Int = 0,
    val outcome: String? = null,
)
