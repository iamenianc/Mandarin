package com.learnhuayu.core.audio.capture

import com.learnhuayu.core.audio.vad.EndOfSpeechDetector
import com.learnhuayu.core.audio.vad.VadEvent

/**
 * A scripted detector: it reports [VadEvent.SpeechEnded] once every [framesPerUtterance]
 * accepted frames and forgets its count on [reset]. The frame count resets only on
 * [reset], so a recorder that fails to reset would derive its next utterance from the
 * previous one.
 */
class FakeEndOfSpeechDetector(
    private val framesPerUtterance: Int = 1,
    private val speechEndedAtMs: Long = 100L,
) : EndOfSpeechDetector {

    var resetCount: Int = 0
        private set

    var acceptedFrames: Int = 0
        private set

    private var framesSinceReset = 0

    override fun accept(frame: ShortArray, length: Int): VadEvent? {
        acceptedFrames++
        framesSinceReset++
        if (framesSinceReset < framesPerUtterance) return null
        return VadEvent.SpeechEnded(speechEndedAtMs)
    }

    override fun reset() {
        resetCount++
        framesSinceReset = 0
    }
}
