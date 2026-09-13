package com.learnhuayu.feature.tones

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
 * The tones module: the four tones and the neutral tone, their contours, tone pairs, and
 * the tone-number notation, taught beginner-first (ADR 0012). Content is the bundled
 * corpus; this module only maps the corpus practices to their drill modes.
 */
class TonesLearningModule @Inject constructor(
    private val contentRepository: BundledContentRepository,
) : LearningModule {
    override val id: String = "tones"
    override val title: String = "Tones"

    override suspend fun lessons(): List<LessonSpec> = contentRepository.module(id)?.lessons().orEmpty()

    override suspend fun practices(): List<PracticeSpec> = contentRepository.module(id)?.practices().orEmpty().map { it.withTonesMode() }

    private fun PracticeSpec.withTonesMode(): PracticeSpec = when (id) {
        TONES_PRACTICE_HEAR_AND_NAME,
        TONES_PRACTICE_TONE_PAIRS,
        TONES_PRACTICE_TONE_NUMBERS,
        -> copy(mode = DrillMode.HEAR_AND_NAME)

        TONES_PRACTICE_SAY_AND_REPEAT -> copy(mode = DrillMode.SPEAK_AND_REPEAT)

        else -> this
    }

    private companion object {
        const val TONES_PRACTICE_HEAR_AND_NAME = "tones-practice-hear-and-name"
        const val TONES_PRACTICE_TONE_PAIRS = "tones-practice-tone-pairs"
        const val TONES_PRACTICE_TONE_NUMBERS = "tones-practice-tone-numbers"
        const val TONES_PRACTICE_SAY_AND_REPEAT = "tones-practice-say-and-repeat"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object TonesModuleBinding {
    @Provides
    @IntoSet
    fun provideTonesLearningModule(module: TonesLearningModule): LearningModule = module
}
