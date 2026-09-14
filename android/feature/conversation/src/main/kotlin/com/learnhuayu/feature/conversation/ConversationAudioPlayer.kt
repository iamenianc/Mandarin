package com.learnhuayu.feature.conversation

/**
 * Plays synthesized speech bytes through the shared audio player. The interface keeps the
 * conversation view model JVM-testable: production writes the bytes to a cache file and
 * plays it, tests substitute a fake.
 */
interface ConversationAudioPlayer {

    suspend fun playSpeech(bytes: ByteArray, contentType: String?)
}
