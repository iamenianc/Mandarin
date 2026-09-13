package com.learnhuayu.core.audio.capture

sealed interface RecorderState {

    data object Idle : RecorderState

    data object Recording : RecorderState

    data class Failed(val message: String) : RecorderState
}
