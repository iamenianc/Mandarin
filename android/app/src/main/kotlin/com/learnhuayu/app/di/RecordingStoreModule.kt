package com.learnhuayu.app.di

import com.learnhuayu.app.audio.CacheRecordingStore
import com.learnhuayu.app.audio.RecordingStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RecordingStoreModule {

    @Binds
    @Singleton
    abstract fun bindRecordingStore(implementation: CacheRecordingStore): RecordingStore
}
