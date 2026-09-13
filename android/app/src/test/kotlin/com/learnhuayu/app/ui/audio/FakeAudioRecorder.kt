package com.learnhuayu.app.ui.audio

import com.learnhuayu.core.audio.capture.AudioRecorder
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.pcm.AudioSpec
import com.learnhuayu.core.audio.pcm.PcmAudio
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAudioRecorder : AudioRecorder {

    private val mutableState = MutableStateFlow<RecorderState>(RecorderState.Idle)
    private val mutableLevel = MutableStateFlow(0f)

    override val state: StateFlow<RecorderState> = mutableState.asStateFlow()

    override val level: StateFlow<Float> = mutableLevel.asStateFlow()

    var startCount = 0
        private set

    var stopCount = 0
        private set

    var stopGate: CompletableDeferred<Unit>? = null

    var stopResult: PcmAudio = PcmAudio(ShortArray(SAMPLE_COUNT), AudioSpec.SAMPLE_RATE_HZ)

    override fun start() {
        startCount++
        mutableState.value = RecorderState.Recording
    }

    override suspend fun stop(): PcmAudio {
        stopCount++
        stopGate?.await()
        mutableState.value = RecorderState.Idle
        return stopResult
    }

    override fun release() = Unit

    fun setLevel(level: Float) {
        mutableLevel.value = level
    }

    fun fail(message: String) {
        mutableState.value = RecorderState.Failed(message)
    }

    private companion object {
        const val SAMPLE_COUNT = 240
    }
}
