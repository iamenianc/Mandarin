package com.learnhuayu.feature.speech

import com.learnhuayu.core.model.LearningModule
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

class SpeechLearningModule : LearningModule {
    override val id: String = "speech"
    override val title: String = "Speech"

    override fun lessons(): List<LessonSpec> = emptyList()

    override fun practices(): List<PracticeSpec> = emptyList()
}

@Module
@InstallIn(SingletonComponent::class)
internal object SpeechModuleBinding {
    @Provides
    @IntoSet
    fun provideSpeechLearningModule(): LearningModule = SpeechLearningModule()
}
