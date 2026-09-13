package com.learnhuayu.core.audio.vad

interface EndOfSpeechDetector {

    fun accept(frame: ShortArray, length: Int = frame.size): VadEvent?

    fun reset()
}
