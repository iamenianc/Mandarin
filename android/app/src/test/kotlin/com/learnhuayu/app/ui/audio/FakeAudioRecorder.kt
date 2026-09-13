package com.learnhuayu.app.ui.audio

import com.learnhuayu.core.audio.capture.AudioRecorder
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.pcm.AudioSpec
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.audio.vad.VadEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAudioRecorder : AudioRecorder {

    private val mutableState = MutableStateFlow<RecorderState>(RecorderState.Idle)
    private val mutableLevel = MutableStateFlow(0f)
    private val mutableVadEvents = MutableSharedFlow<VadEvent>(extraBufferCapacity = VAD_BUFFER_CAPACITY)

    override val state: StateFlow<RecorderState> = mutableState.asStateFlow()

    override val level: StateFlow<Float> = mutableLevel.asStateFlow()

    override val vadEvents: Flow<VadEvent> = mutableVadEvents.asSharedFlow()

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

    fun signalSpeechEnded(atMs: Long = 0L) {
        mutableVadEvents.tryEmit(VadEvent.SpeechEnded(atMs))
    }

    fun signalSpeechStarted(atMs: Long = 0L) {
        mutableVadEvents.tryEmit(VadEvent.SpeechStarted(atMs))
    }

    private companion object {
        const val SAMPLE_COUNT = 240
        const val VAD_BUFFER_CAPACITY = 8
    }
}
