package com.learnhuayu.feature.fundamentals

import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.data.content.ContentModule
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FundamentalsLearningModuleTest {

    private val module = FundamentalsLearningModule(FakeBundledContentRepository(fundamentalsModule))

    @Test
    fun `lessons come from the bundled fundamentals module`() = runTest {
        assertEquals(
            listOf(
                "fundamentals-01-syllable-anatomy",
                "fundamentals-02-initials",
                "fundamentals-03-finals",
                "fundamentals-04-tone-numbers",
                "fundamentals-05-tone-sandhi",
            ),
            module.lessons().map { it.id },
        )
    }

    @Test
    fun `practices map their ids to the expected drill modes`() = runTest {
        val modes = module.practices().associate { it.id to it.mode }

        assertEquals(
            mapOf(
                "fundamentals-practice-syllable-parts" to DrillMode.LISTEN_AND_CHOOSE,
                "fundamentals-practice-initials" to DrillMode.LISTEN_AND_CHOOSE,
                "fundamentals-practice-finals" to DrillMode.LISTEN_AND_CHOOSE,
                "fundamentals-practice-tone-numbers" to DrillMode.HEAR_AND_NAME,
                "fundamentals-practice-tone-sandhi" to DrillMode.HEAR_AND_NAME,
                "fundamentals-practice-spelling-conventions" to DrillMode.SPEAK_AND_REPEAT,
            ),
            modes,
        )
    }

    @Test
    fun `an unmapped practice keeps its default mode`() = runTest {
        val other = FundamentalsLearningModule(FakeBundledContentRepository(unmappedModule))
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

private val fundamentalsModule = ContentModule(
    id = "fundamentals",
    title = "Fundamentals",
    theme = "Syllable anatomy and the pinyin sound system, taught by ear.",
    lessonSpecs = listOf(
        lesson("fundamentals-01-syllable-anatomy"),
        lesson("fundamentals-02-initials"),
        lesson("fundamentals-03-finals"),
        lesson("fundamentals-04-tone-numbers"),
        lesson("fundamentals-05-tone-sandhi"),
    ),
    practiceSpecs = listOf(
        practice("fundamentals-practice-syllable-parts"),
        practice("fundamentals-practice-initials"),
        practice("fundamentals-practice-finals"),
        practice("fundamentals-practice-tone-numbers"),
        practice("fundamentals-practice-tone-sandhi"),
        practice("fundamentals-practice-spelling-conventions"),
    ),
)

private val unmappedModule = fundamentalsModule.copy(
    practiceSpecs = listOf(practice("fundamentals-practice-unknown")),
)

private fun lesson(id: String): LessonSpec = LessonSpec(
    id = id,
    moduleId = "fundamentals",
    title = id,
    level = "beginner",
    topic = "fundamentals",
    contentItemIds = listOf("fundamentals-ma1"),
)

private fun practice(id: String): PracticeSpec = PracticeSpec(
    id = id,
    moduleId = "fundamentals",
    title = id,
    contentItemIds = listOf("fundamentals-ma1"),
)
