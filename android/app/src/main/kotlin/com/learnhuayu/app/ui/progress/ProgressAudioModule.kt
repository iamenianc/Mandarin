package com.learnhuayu.app.ui.progress

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the cache-file speech player behind the progress audio seam (JVM-testable). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProgressAudioModule {

    @Binds
    abstract fun bindProgressAudioPlayer(implementation: CacheFileProgressAudioPlayer): ProgressAudioPlayer
}
