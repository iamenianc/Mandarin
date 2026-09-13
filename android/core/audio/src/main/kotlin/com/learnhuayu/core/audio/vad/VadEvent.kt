package com.learnhuayu.core.audio.vad

sealed interface VadEvent {

    data class SpeechStarted(val atMs: Long) : VadEvent

    data class SpeechEnded(val atMs: Long) : VadEvent
}
