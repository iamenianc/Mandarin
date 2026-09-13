package com.learnhuayu.core.assessment.tone

import kotlinx.serialization.Serializable

@Serializable
enum class ContourDirection {
    LEVEL,
    RISING,
    FALLING,
    DIP,
    UNKNOWN,
}
