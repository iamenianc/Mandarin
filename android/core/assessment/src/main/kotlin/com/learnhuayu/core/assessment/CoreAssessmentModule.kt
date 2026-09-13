package com.learnhuayu.core.assessment

import com.learnhuayu.core.assessment.dsp.AcousticFeatureExtractor
import com.learnhuayu.core.assessment.dsp.BaselineAcousticFeatureExtractor
import com.learnhuayu.core.assessment.evidence.ReferenceFeaturePrecomputer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CoreAssessmentModule {

    @Provides
    @Singleton
    fun provideAcousticFeatureExtractor(extractor: BaselineAcousticFeatureExtractor): AcousticFeatureExtractor = extractor

    @Provides
    @Singleton
    fun provideBaselineAcousticFeatureExtractor(): BaselineAcousticFeatureExtractor = BaselineAcousticFeatureExtractor()

    @Provides
    @Singleton
    fun provideToneAssessmentPipeline(
        extractor: AcousticFeatureExtractor,
    ): ToneAssessmentPipeline = ToneAssessmentPipeline(extractor = extractor)

    @Provides
    @Singleton
    fun provideReferenceFeaturePrecomputer(
        extractor: AcousticFeatureExtractor,
    ): ReferenceFeaturePrecomputer = ReferenceFeaturePrecomputer(extractor = extractor)
}
