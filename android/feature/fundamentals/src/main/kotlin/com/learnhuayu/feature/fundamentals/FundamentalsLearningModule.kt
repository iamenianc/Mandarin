package com.learnhuayu.feature.fundamentals

import com.learnhuayu.core.model.LearningModule
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

class FundamentalsLearningModule : LearningModule {
    override val id: String = "fundamentals"
    override val title: String = "Fundamentals"

    override fun lessons(): List<LessonSpec> = emptyList()

    override fun practices(): List<PracticeSpec> = emptyList()
}

@Module
@InstallIn(SingletonComponent::class)
internal object FundamentalsModuleBinding {
    @Provides
    @IntoSet
    fun provideFundamentalsLearningModule(): LearningModule = FundamentalsLearningModule()
}
