package com.learnhuayu.core.audio.playback

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {

    val state: StateFlow<PlaybackState>

    fun play(source: AudioSource)

    fun pause()

    fun replay()

    fun seekTo(positionMs: Long)

    fun release()
}
