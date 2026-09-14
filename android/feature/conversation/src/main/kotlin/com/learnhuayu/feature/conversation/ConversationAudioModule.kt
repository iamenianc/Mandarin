package com.learnhuayu.feature.conversation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the cache-file speech player behind the conversation audio seam (JVM-testable). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConversationAudioModule {

    @Binds
    abstract fun bindConversationAudioPlayer(implementation: CacheFileConversationAudioPlayer): ConversationAudioPlayer
}
