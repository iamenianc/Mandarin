package com.learnhuayu.feature.tones

import com.learnhuayu.core.model.LearningModule
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

class TonesLearningModule : LearningModule {
    override val id: String = "tones"
    override val title: String = "Tones"

    override fun lessons(): List<LessonSpec> = emptyList()

    override fun practices(): List<PracticeSpec> = emptyList()
}

@Module
@InstallIn(SingletonComponent::class)
internal object TonesModuleBinding {
    @Provides
    @IntoSet
    fun provideTonesLearningModule(): LearningModule = TonesLearningModule()
}
