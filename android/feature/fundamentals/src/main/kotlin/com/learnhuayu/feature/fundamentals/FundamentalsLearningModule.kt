package com.learnhuayu.feature.fundamentals

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
 * The fundamentals module: syllable anatomy, the pinyin sound system (initials, finals,
 * spelling conventions, tone numbers), and tone sandhi, taught by ear (ADR 0011, ADR 0012).
 * Content is the bundled corpus; this module only maps the corpus practices to their drill
 * modes.
 */
class FundamentalsLearningModule @Inject constructor(
    private val contentRepository: BundledContentRepository,
) : LearningModule {
    override val id: String = "fundamentals"
    override val title: String = "Fundamentals"

    override suspend fun lessons(): List<LessonSpec> = contentRepository.module(id)?.lessons().orEmpty()

    override suspend fun practices(): List<PracticeSpec> = contentRepository.module(id)?.practices().orEmpty().map { it.withFundamentalsMode() }

    private fun PracticeSpec.withFundamentalsMode(): PracticeSpec = when (id) {
        FUNDAMENTALS_PRACTICE_TONE_NUMBERS,
        FUNDAMENTALS_PRACTICE_TONE_SANDHI,
        -> copy(mode = DrillMode.HEAR_AND_NAME)

        FUNDAMENTALS_PRACTICE_SPELLING_CONVENTIONS -> copy(mode = DrillMode.SPEAK_AND_REPEAT)

        else -> this
    }

    private companion object {
        const val FUNDAMENTALS_PRACTICE_TONE_NUMBERS = "fundamentals-practice-tone-numbers"
        const val FUNDAMENTALS_PRACTICE_TONE_SANDHI = "fundamentals-practice-tone-sandhi"
        const val FUNDAMENTALS_PRACTICE_SPELLING_CONVENTIONS = "fundamentals-practice-spelling-conventions"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object FundamentalsModuleBinding {
    @Provides
    @IntoSet
    fun provideFundamentalsLearningModule(module: FundamentalsLearningModule): LearningModule = module
}
