package com.learnhuayu.feature.speech

import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.data.content.ContentModule
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the practice-to-mode mapping for the bundled speech corpus
 * (`assets/content/speech/lessons.json`): the two say-and-repeat practices run the
 * coached speak-and-repeat mode, and every lesson stays browse-only.
 */
class SpeechLearningModuleTest {

    private val module = SpeechLearningModule(FakeBundledContentRepository(speechModule))

    @Test
    fun `lessons come from the bundled speech module`() = runTest {
        assertEquals(
            listOf("speech-01-say-the-tone", "speech-02-say-the-word"),
            module.lessons().map { it.id },
        )
    }

    @Test
    fun `production practices map to speak and repeat with feedback`() = runTest {
        val modes = module.practices().associate { it.id to it.mode }

        assertEquals(
            mapOf(
                "speech-practice-say-the-tone" to DrillMode.SPEAK_AND_REPEAT_FEEDBACK,
                "speech-practice-say-the-word" to DrillMode.SPEAK_AND_REPEAT_FEEDBACK,
            ),
            modes,
        )
    }

    @Test
    fun `an unmapped practice keeps its default mode`() = runTest {
        val other = SpeechLearningModule(FakeBundledContentRepository(unmappedModule))
        val practice = other.practices().single()

        assertEquals(DrillMode.LISTEN_AND_CHOOSE, practice.mode)
    }
}

private class FakeBundledContentRepository(
    private val contentModule: ContentModule,
) : BundledContentRepository {
    override suspend fun modules(): List<ContentModule> = listOf(contentModule)

    override suspend fun module(moduleId: String): ContentModule? = contentModule.takeIf { it.id == moduleId }

    override suspend fun contentItem(contentItemId: String): ContentItem? = null

    override suspend fun contentItems(contentItemIds: List<String>): List<ContentItem> = emptyList()

    override fun audioAssetPath(audioAssetRef: String): String = "audio/$audioAssetRef"
}

private val speechModule = ContentModule(
    id = "speech",
    title = "Speech",
    theme = "Say-and-repeat production drills with compared feedback.",
    lessonSpecs = listOf(
        LessonSpec(
            id = "speech-01-say-the-tone",
            moduleId = "speech",
            title = "Say the tone",
            level = "beginner",
            topic = "tone production",
            contentItemIds = listOf("speech-ma1"),
        ),
        LessonSpec(
            id = "speech-02-say-the-word",
            moduleId = "speech",
            title = "Say the word",
            level = "beginner",
            topic = "word production",
            contentItemIds = listOf("speech-ni3-hao3"),
        ),
    ),
    practiceSpecs = listOf(
        practice("speech-practice-say-the-tone"),
        practice("speech-practice-say-the-word"),
    ),
)

private val unmappedModule = speechModule.copy(
    practiceSpecs = listOf(practice("speech-practice-unknown")),
)

private fun practice(id: String): PracticeSpec = PracticeSpec(
    id = id,
    moduleId = "speech",
    title = id,
    contentItemIds = listOf("speech-ma1"),
)
