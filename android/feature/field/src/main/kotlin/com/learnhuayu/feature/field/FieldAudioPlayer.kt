package com.learnhuayu.feature.field

/**
 * Plays synthesized speech bytes through the shared audio player. The interface keeps the
 * field view model JVM-testable: production writes the bytes to a cache file and plays it,
 * tests substitute a fake.
 */
interface FieldAudioPlayer {

    suspend fun playSpeech(bytes: ByteArray, contentType: String?)
}
