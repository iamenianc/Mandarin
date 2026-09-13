package com.learnhuayu.core.model

/**
 * How the shared drill engine runs one lesson or practice sequence. Lessons browse and
 * replay; practice modes add a response step. Both are driven by the same [ContentItem]
 * stream (ADR 0007).
 */
enum class DrillMode {
    LESSON,
    HEAR_AND_NAME,
    LISTEN_AND_CHOOSE,
    SPEAK_AND_REPEAT,
}
