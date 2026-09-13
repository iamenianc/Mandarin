package com.learnhuayu.core.audio.playback

data class PlaybackState(
    val source: AudioSource? = null,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
) {
    val isPlaying: Boolean
        get() = status == PlaybackStatus.Playing
}

sealed interface PlaybackStatus {

    data object Idle : PlaybackStatus

    data object Buffering : PlaybackStatus

    data object Playing : PlaybackStatus

    data object Paused : PlaybackStatus

    data object Ended : PlaybackStatus

    data class Failed(val message: String) : PlaybackStatus
}
