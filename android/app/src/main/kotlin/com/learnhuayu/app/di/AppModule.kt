package com.learnhuayu.app.di

import com.learnhuayu.app.ui.session.AssetReferenceClipReader
import com.learnhuayu.app.ui.session.AttemptEvidenceSource
import com.learnhuayu.app.ui.session.NoOpAttemptEvidenceSource
import com.learnhuayu.app.ui.session.ReferenceClipReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideReferenceClipReader(reader: AssetReferenceClipReader): ReferenceClipReader = reader

    @Provides
    @Singleton
    fun provideAttemptEvidenceSource(source: NoOpAttemptEvidenceSource): AttemptEvidenceSource = source
}
