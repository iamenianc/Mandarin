package com.learnhuayu.core.audio.capture

import com.learnhuayu.core.audio.pcm.PcmAudio
import kotlinx.coroutines.flow.StateFlow

interface AudioRecorder {

    val state: StateFlow<RecorderState>

    val level: StateFlow<Float>

    fun start()

    suspend fun stop(): PcmAudio

    fun release()
}
