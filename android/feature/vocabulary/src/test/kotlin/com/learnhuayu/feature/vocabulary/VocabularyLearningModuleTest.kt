package com.learnhuayu.feature.vocabulary

import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.data.content.ContentModule
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class VocabularyLearningModuleTest {

    private val module = VocabularyLearningModule(FakeBundledContentRepository(vocabularyModule))

    @Test
    fun `lessons come from the bundled vocabulary module`() = runTest {
        assertEquals(vocabularyLessonIds, module.lessons().map { it.id })
    }

    @Test
    fun `every corpus practice maps to the expected drill mode`() = runTest {
        val modes = module.practices().associate { it.id to it.mode }

        assertEquals(
            mapOf(
                "vocabulary-practice-beginner" to DrillMode.LISTEN_AND_CHOOSE,
                "vocabulary-practice-tourist" to DrillMode.LISTEN_AND_CHOOSE,
                "vocabulary-practice-survival" to DrillMode.LISTEN_AND_CHOOSE,
            ),
            modes,
        )
    }

    @Test
    fun `an unmapped practice keeps its default mode`() = runTest {
        val other = VocabularyLearningModule(FakeBundledContentRepository(unmappedModule))
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

private val vocabularyLessonIds = listOf(
    "vocabulary-01-greetings",
    "vocabulary-02-courtesy",
    "vocabulary-03-numbers",
    "vocabulary-04-time",
    "vocabulary-05-ordering-food",
    "vocabulary-06-directions",
    "vocabulary-07-transport",
    "vocabulary-08-hotel",
    "vocabulary-09-shopping",
    "vocabulary-10-emergencies",
    "vocabulary-11-repair",
)

private val vocabularyModule = ContentModule(
    id = "vocabulary",
    title = "Vocabulary",
    theme = "Audio-first beginner, tourist, and survival vocabulary.",
    lessonSpecs = vocabularyLessonIds.map(::lesson),
    practiceSpecs = listOf(
        practice("vocabulary-practice-beginner"),
        practice("vocabulary-practice-tourist"),
        practice("vocabulary-practice-survival"),
    ),
)

private val unmappedModule = vocabularyModule.copy(
    practiceSpecs = listOf(practice("vocabulary-practice-unknown")),
)

private fun lesson(id: String): LessonSpec = LessonSpec(
    id = id,
    moduleId = "vocabulary",
    title = id,
    level = "beginner",
    topic = "vocabulary",
    contentItemIds = listOf("vocabulary-ni3-hao3"),
)

private fun practice(id: String): PracticeSpec = PracticeSpec(
    id = id,
    moduleId = "vocabulary",
    title = id,
    contentItemIds = listOf("vocabulary-ni3-hao3"),
)
