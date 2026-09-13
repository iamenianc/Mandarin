package com.learnhuayu.core.audio.capture

import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.audio.vad.VadEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AudioRecorder {

    val state: StateFlow<RecorderState>

    val level: StateFlow<Float>

    /**
     * Voice-activity transitions observed while capturing, when a detector is configured.
     * The recorder only signals; it never stops itself. The flow is empty for a recorder
     * built without a detector, and it does not replay events to collectors that attach
     * after the transition.
     */
    val vadEvents: Flow<VadEvent>

    fun start()

    suspend fun stop(): PcmAudio

    fun release()
}
