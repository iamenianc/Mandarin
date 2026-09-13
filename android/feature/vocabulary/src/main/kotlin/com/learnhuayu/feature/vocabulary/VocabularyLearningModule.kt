package com.learnhuayu.feature.vocabulary

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
 * The vocabulary module: beginner, tourist, and survival word/phrase sets taught audio-first
 * with pinyin always shown (ADR 0011). Content is the bundled corpus; this module only maps
 * the corpus practices to their drill modes.
 *
 * The corpus practices identify words and phrases from audio by pinyin or meaning, so they
 * run as [DrillMode.LISTEN_AND_CHOOSE]. A tone-number answer would run as
 * [DrillMode.HEAR_AND_NAME] and a read-aloud practice as [DrillMode.SPEAK_AND_REPEAT];
 * the vocabulary corpus contains neither.
 */
class VocabularyLearningModule @Inject constructor(
    private val contentRepository: BundledContentRepository,
) : LearningModule {
    override val id: String = "vocabulary"
    override val title: String = "Vocabulary"

    override suspend fun lessons(): List<LessonSpec> = contentRepository.module(id)?.lessons().orEmpty()

    override suspend fun practices(): List<PracticeSpec> = contentRepository.module(id)?.practices().orEmpty().map { it.withVocabularyMode() }

    private fun PracticeSpec.withVocabularyMode(): PracticeSpec = when (id) {
        VOCABULARY_PRACTICE_BEGINNER,
        VOCABULARY_PRACTICE_TOURIST,
        VOCABULARY_PRACTICE_SURVIVAL,
        -> copy(mode = DrillMode.LISTEN_AND_CHOOSE)

        else -> this
    }

    private companion object {
        const val VOCABULARY_PRACTICE_BEGINNER = "vocabulary-practice-beginner"
        const val VOCABULARY_PRACTICE_TOURIST = "vocabulary-practice-tourist"
        const val VOCABULARY_PRACTICE_SURVIVAL = "vocabulary-practice-survival"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object VocabularyModuleBinding {
    @Provides
    @IntoSet
    fun provideVocabularyLearningModule(module: VocabularyLearningModule): LearningModule = module
}
