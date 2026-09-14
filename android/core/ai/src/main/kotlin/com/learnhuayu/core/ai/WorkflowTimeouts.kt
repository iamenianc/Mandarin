package com.learnhuayu.core.ai

/**
 * Client-side timeout budgets for each AI workflow (FR-23, NFR-1, NFR-2).
 *
 * Every value mirrors the latency budget in `docs/08-ai-workflows.md`: WF-1 serves NFR-1
 * (~2s), WF-2 and WF-10 serve NFR-2 (~3s), WF-4 and WF-7 use the upper bound of their
 * documented ranges, WF-8 and WF-9 use the upper bound of `~2-4s`, WF-3 is playback-bound
 * so it gets a bounded 10s, and WF-5 runs off the critical path so it gets a generous 15s.
 *
 * A timeout surfaces as [WorkflowFailure.Timeout] (never thrown), so every existing caller
 * fallback path triggers unchanged: each call site already treats any
 * [WorkflowResult.Failure] as offline/fallback.
 *
 * WF-8 compatibility: `SessionViewModel` keeps its 10s `withTimeout` outer cap around the
 * WF-8 call. The inner 4s timeout below wins first and maps to [WorkflowResult.Failure];
 * the outer 10s cap remains a backstop for admission and validation overhead above the
 * workflow layer.
 */
data class WorkflowTimeouts(
    val pronunciationFeedbackMillis: Long = PRONUNCIATION_FEEDBACK_TIMEOUT_MS,
    val conversationTurnMillis: Long = CONVERSATION_TURN_TIMEOUT_MS,
    val speechSynthesisMillis: Long = SPEECH_SYNTHESIS_TIMEOUT_MS,
    val responseTranscriptionMillis: Long = RESPONSE_TRANSCRIPTION_TIMEOUT_MS,
    val progressSummaryMillis: Long = PROGRESS_SUMMARY_TIMEOUT_MS,
    val mandarinQaMillis: Long = MANDARIN_QA_TIMEOUT_MS,
    val exerciseGenerationMillis: Long = EXERCISE_GENERATION_TIMEOUT_MS,
    val fieldMissionGenerationMillis: Long = FIELD_MISSION_GENERATION_TIMEOUT_MS,
    val localTurnMillis: Long = LOCAL_TURN_TIMEOUT_MS,
) {
    companion object {
        /** WF-1 pronunciation feedback: NFR-1 (~2s). */
        const val PRONUNCIATION_FEEDBACK_TIMEOUT_MS = 2_000L

        /** WF-2 conversation turn: NFR-2 (~3s). */
        const val CONVERSATION_TURN_TIMEOUT_MS = 3_000L

        /** WF-3 speech synthesis: playback-bound, bounded at 10s for TTS audio download. */
        const val SPEECH_SYNTHESIS_TIMEOUT_MS = 10_000L

        /** WF-4 response transcription: `~1-2s` upper bound. */
        const val RESPONSE_TRANSCRIPTION_TIMEOUT_MS = 2_000L

        /** WF-5 progress summary: off the critical path, generous 15s. */
        const val PROGRESS_SUMMARY_TIMEOUT_MS = 15_000L

        /** WF-7 Raymond Q and A: `~2-3s` upper bound. */
        const val MANDARIN_QA_TIMEOUT_MS = 3_000L

        /** WF-8 exercise generation: `~2-4s` upper bound; wins over the 10s outer cap. */
        const val EXERCISE_GENERATION_TIMEOUT_MS = 4_000L

        /** WF-9 field mission generation: `~2-4s` upper bound. */
        const val FIELD_MISSION_GENERATION_TIMEOUT_MS = 4_000L

        /** WF-10 local conversation turn: NFR-2 (~3s). */
        const val LOCAL_TURN_TIMEOUT_MS = 3_000L
    }
}
