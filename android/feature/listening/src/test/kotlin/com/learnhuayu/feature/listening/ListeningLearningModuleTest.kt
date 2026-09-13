package com.learnhuayu.feature.listening

import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.data.content.ContentModule
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.DrillMode
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningLearningModuleTest {

    private val module = ListeningLearningModule(FakeBundledContentRepository(listeningModule))

    @Test
    fun `lessons come from the bundled listening module`() = runTest {
        assertEquals(
            listOf("listening-01-hear-the-tone", "listening-02-hear-the-word"),
            module.lessons().map { it.id },
        )
    }

    /**
     * The corpus shapes fix the mapping: the hear-the-tone practice draws on word and
     * minimalPair items whose targetTones are the answer, so the learner hears audio and
     * names the tone (HEAR_AND_NAME). The hear-the-word practice draws on phrase items whose
     * answer is the matching pinyin or meaning, so the learner selects a choice
     * (LISTEN_AND_CHOOSE). Both remain tap-only and offline.
     */
    @Test
    fun `practices map their ids to the expected drill modes`() = runTest {
        val modes = module.practices().associate { it.id to it.mode }

        assertEquals(
            mapOf(
                "listening-practice-hear-the-tone" to DrillMode.HEAR_AND_NAME,
                "listening-practice-hear-the-word" to DrillMode.LISTEN_AND_CHOOSE,
            ),
            modes,
        )
    }

    @Test
    fun `an unmapped practice keeps its default mode`() = runTest {
        val other = ListeningLearningModule(FakeBundledContentRepository(unmappedModule))
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

private val listeningModule = ContentModule(
    id = "listening",
    title = "Listening",
    theme = "Audio-first hear-and-identify drills.",
    lessonSpecs = listOf(
        LessonSpec(
            id = "listening-01-hear-the-tone",
            moduleId = "listening",
            title = "Hear the tone",
            level = "beginner",
            topic = "tone identification",
            contentItemIds = listOf("listening-ma1", "listening-ma2", "listening-ma3", "listening-ma4", "listening-ma5"),
        ),
        LessonSpec(
            id = "listening-02-hear-the-word",
            moduleId = "listening",
            title = "Hear the word",
            level = "beginner",
            topic = "word identification",
            contentItemIds = listOf("listening-ni3-hao3", "listening-xie4-xie5", "listening-zai4-jian4", "listening-dui4-bu5-qi3"),
        ),
    ),
    practiceSpecs = listOf(
        practice("listening-practice-hear-the-tone"),
        practice("listening-practice-hear-the-word"),
    ),
)

private val unmappedModule = listeningModule.copy(
    practiceSpecs = listOf(practice("listening-practice-unknown")),
)

private fun practice(id: String): PracticeSpec = PracticeSpec(
    id = id,
    moduleId = "listening",
    title = id,
    contentItemIds = listOf("listening-ma1"),
)
