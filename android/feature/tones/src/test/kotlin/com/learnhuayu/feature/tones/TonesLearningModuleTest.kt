package com.learnhuayu.feature.tones

import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.data.content.ContentModule
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TonesLearningModuleTest {

    private val module = TonesLearningModule(FakeBundledContentRepository(tonesModule))

    @Test
    fun `lessons come from the bundled tones module`() = runTest {
        assertEquals(listOf("tones-01-first-tone"), module.lessons().map { it.id })
    }

    @Test
    fun `practices map their ids to the expected drill modes`() = runTest {
        val modes = module.practices().associate { it.id to it.mode }

        assertEquals(
            mapOf(
                "tones-practice-hear-and-name" to DrillMode.HEAR_AND_NAME,
                "tones-practice-tone-pairs" to DrillMode.HEAR_AND_NAME,
                "tones-practice-tone-numbers" to DrillMode.HEAR_AND_NAME,
                "tones-practice-say-and-repeat" to DrillMode.SPEAK_AND_REPEAT,
            ),
            modes,
        )
    }

    @Test
    fun `an unmapped practice keeps its default mode`() = runTest {
        val other = TonesLearningModule(FakeBundledContentRepository(unmappedModule))
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

private val tonesModule = ContentModule(
    id = "tones",
    title = "Tones",
    theme = "Tone contours and the tone-number notation.",
    lessonSpecs = listOf(
        LessonSpec(
            id = "tones-01-first-tone",
            moduleId = "tones",
            title = "First tone",
            level = "beginner",
            topic = "tone contours",
            contentItemIds = listOf("tones-ma1"),
        ),
    ),
    practiceSpecs = listOf(
        practice("tones-practice-hear-and-name"),
        practice("tones-practice-tone-pairs"),
        practice("tones-practice-tone-numbers"),
        practice("tones-practice-say-and-repeat"),
    ),
)

private val unmappedModule = tonesModule.copy(
    practiceSpecs = listOf(practice("tones-practice-unknown")),
)

private fun practice(id: String): PracticeSpec = PracticeSpec(
    id = id,
    moduleId = "tones",
    title = id,
    contentItemIds = listOf("tones-ma1"),
)
