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

    /**
     * Speak-and-repeat with a coached feedback step: record, write the attempt, then ask
     * WF-1 to compare the reference clip with the attempt and render the coaching (ADR 0005,
     * ADR 0014). The reference-vs-attempt comparison degrades to the offline fallback when
     * the reference clip is missing or the Worker is unreachable.
     */
    SPEAK_AND_REPEAT_FEEDBACK,
}
