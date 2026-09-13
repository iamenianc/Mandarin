package com.learnhuayu.core.ai

enum class AudioFormat(val wireName: String) {
    WAV("wav"),
    MP3("mp3"),
    M4A("m4a"),
    OGG("ogg"),
    WEBM("webm"),
    FLAC("flac"),
}

data class AudioClip(
    val bytes: ByteArray,
    val format: AudioFormat,
)
