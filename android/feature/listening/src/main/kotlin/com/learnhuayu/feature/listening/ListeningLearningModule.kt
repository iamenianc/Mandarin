package com.learnhuayu.feature.listening

import com.learnhuayu.core.model.LearningModule
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

class ListeningLearningModule : LearningModule {
    override val id: String = "listening"
    override val title: String = "Listening"

    override fun lessons(): List<LessonSpec> = emptyList()

    override fun practices(): List<PracticeSpec> = emptyList()
}

@Module
@InstallIn(SingletonComponent::class)
internal object ListeningModuleBinding {
    @Provides
    @IntoSet
    fun provideListeningLearningModule(): LearningModule = ListeningLearningModule()
}
