package com.learnhuayu.app.ui.progress

/**
 * Plays synthesized speech bytes through the shared audio player. The interface keeps the
 * progress view model JVM-testable: production writes the bytes to a cache file and plays
 * them, tests substitute a fake. WF-3 failure is silent, so this never surfaces an error.
 */
interface ProgressAudioPlayer {

    suspend fun playSpeech(bytes: ByteArray, contentType: String?)
}
