package com.learnhuayu.core.audio.playback

sealed interface AudioSource {

    data class Asset(val path: String) : AudioSource

    data class LocalFile(val path: String) : AudioSource

    data class UriSource(val uri: String) : AudioSource
}
