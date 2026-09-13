package com.learnhuayu.feature.vocabulary

import com.learnhuayu.core.model.LearningModule
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

class VocabularyLearningModule : LearningModule {
    override val id: String = "vocabulary"
    override val title: String = "Vocabulary"

    override suspend fun lessons(): List<LessonSpec> = emptyList()

    override suspend fun practices(): List<PracticeSpec> = emptyList()
}

@Module
@InstallIn(SingletonComponent::class)
internal object VocabularyModuleBinding {
    @Provides
    @IntoSet
    fun provideVocabularyLearningModule(): LearningModule = VocabularyLearningModule()
}
