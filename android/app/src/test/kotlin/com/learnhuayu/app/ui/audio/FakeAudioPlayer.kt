package com.learnhuayu.app.ui.audio

import com.learnhuayu.core.audio.playback.AudioPlayer
import com.learnhuayu.core.audio.playback.AudioSource
import com.learnhuayu.core.audio.playback.PlaybackState
import com.learnhuayu.core.audio.playback.PlaybackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAudioPlayer : AudioPlayer {

    private val mutableState = MutableStateFlow(PlaybackState())

    override val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    val playedSources = mutableListOf<AudioSource>()

    var pauseCount = 0
        private set

    override fun play(source: AudioSource) {
        playedSources += source
        mutableState.value = PlaybackState(source = source, status = PlaybackStatus.Playing)
    }

    override fun pause() {
        pauseCount++
        mutableState.value = mutableState.value.copy(status = PlaybackStatus.Paused)
    }

    override fun replay() = Unit

    override fun seekTo(positionMs: Long) = Unit

    override fun release() = Unit

    fun emit(state: PlaybackState) {
        mutableState.value = state
    }
}
