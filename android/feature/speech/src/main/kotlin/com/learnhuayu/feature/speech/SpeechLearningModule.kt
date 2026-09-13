package com.learnhuayu.feature.speech

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
 * The speech module: audio-first say-and-repeat production drills over tones and words. Each
 * practice records the learner, plays the attempt back, and asks WF-1 for compared
 * reference-vs-attempt coaching (ADR 0005, ADR 0014). Content is the bundled corpus; this
 * module only maps the corpus practices to their drill modes.
 */
class SpeechLearningModule @Inject constructor(
    private val contentRepository: BundledContentRepository,
) : LearningModule {
    override val id: String = "speech"
    override val title: String = "Speech"

    override suspend fun lessons(): List<LessonSpec> = contentRepository.module(id)?.lessons().orEmpty()

    override suspend fun practices(): List<PracticeSpec> = contentRepository.module(id)?.practices().orEmpty().map { it.withSpeechMode() }

    private fun PracticeSpec.withSpeechMode(): PracticeSpec = when (id) {
        SPEECH_PRACTICE_SAY_THE_TONE,
        SPEECH_PRACTICE_SAY_THE_WORD,
        -> copy(mode = DrillMode.SPEAK_AND_REPEAT_FEEDBACK)

        else -> this
    }

    private companion object {
        const val SPEECH_PRACTICE_SAY_THE_TONE = "speech-practice-say-the-tone"
        const val SPEECH_PRACTICE_SAY_THE_WORD = "speech-practice-say-the-word"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object SpeechModuleBinding {
    @Provides
    @IntoSet
    fun provideSpeechLearningModule(module: SpeechLearningModule): LearningModule = module
}
