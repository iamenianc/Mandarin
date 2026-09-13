package com.learnhuayu.core.model

data class MissionTurn(
    val id: String,
    val sessionId: String,
    val speaker: MissionSpeaker,
    val audioRef: String? = null,
    val transcript: String? = null,
)

enum class MissionSpeaker {
    LEARNER,
    LOCAL,
}
