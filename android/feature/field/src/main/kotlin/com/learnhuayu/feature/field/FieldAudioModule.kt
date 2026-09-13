package com.learnhuayu.feature.field

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the cache-file speech player behind the field audio seam (JVM-testable). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class FieldAudioModule {

    @Binds
    abstract fun bindFieldAudioPlayer(implementation: CacheFileFieldAudioPlayer): FieldAudioPlayer
}
