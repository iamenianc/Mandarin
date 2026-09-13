package com.learnhuayu.feature.listening

import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.LearningModule
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * The listening module: audio-first hear-and-identify drills that name a tone then choose the
 * matching pinyin or meaning, with no speaking required (FR-10). Content is the bundled
 * corpus; this module only maps the corpus practices to their drill modes.
 */
class ListeningLearningModule @Inject constructor(
    private val contentRepository: BundledContentRepository,
) : LearningModule {
    override val id: String = "listening"
    override val title: String = "Listening"

    override suspend fun lessons(): List<LessonSpec> = contentRepository.module(id)?.lessons().orEmpty()

    override suspend fun practices(): List<PracticeSpec> = contentRepository.module(id)?.practices().orEmpty().map { it.withListeningMode() }

    private fun PracticeSpec.withListeningMode(): PracticeSpec = when (id) {
        LISTENING_PRACTICE_HEAR_THE_TONE -> copy(mode = DrillMode.HEAR_AND_NAME)

        LISTENING_PRACTICE_HEAR_THE_WORD -> copy(mode = DrillMode.LISTEN_AND_CHOOSE)

        else -> this
    }

    private companion object {
        const val LISTENING_PRACTICE_HEAR_THE_TONE = "listening-practice-hear-the-tone"
        const val LISTENING_PRACTICE_HEAR_THE_WORD = "listening-practice-hear-the-word"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object ListeningModuleBinding {
    @Provides
    @IntoSet
    fun provideListeningLearningModule(module: ListeningLearningModule): LearningModule = module
}
