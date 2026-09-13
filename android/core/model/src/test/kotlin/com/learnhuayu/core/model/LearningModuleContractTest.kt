package com.learnhuayu.core.model

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class LearningModuleContractTest {

    private class FakeModule : LearningModule {
        override val id: String = "fake"
        override val title: String = "Fake"

        override suspend fun lessons(): List<LessonSpec> = listOf(
            LessonSpec(
                id = "fake-lesson",
                moduleId = id,
                title = "First lesson",
                level = "beginner",
                topic = "greetings",
                contentItemIds = listOf("phrase-ni3-hao3"),
            ),
        )

        override suspend fun practices(): List<PracticeSpec> = listOf(
            PracticeSpec(
                id = "fake-practice",
                moduleId = id,
                title = "First practice",
                contentItemIds = listOf("phrase-ni3-hao3"),
                mode = DrillMode.HEAR_AND_NAME,
            ),
        )
    }

    @Test
    fun `module exposes its lessons and practices`() = runTest {
        val module = FakeModule()

        assertEquals("fake", module.lessons().single().moduleId)
        assertEquals("fake", module.practices().single().moduleId)
        assertEquals(listOf("phrase-ni3-hao3"), module.lessons().single().contentItemIds)
    }

    @Test
    fun `practice spec defaults to listen and choose and carries an explicit mode`() = runTest {
        val module = FakeModule()

        assertEquals(DrillMode.HEAR_AND_NAME, module.practices().single().mode)
        assertEquals(
            DrillMode.LISTEN_AND_CHOOSE,
            PracticeSpec(
                id = "fake-default",
                moduleId = "fake",
                title = "Default mode",
                contentItemIds = listOf("phrase-ni3-hao3"),
            ).mode,
        )
    }

    @Test
    fun `content item carries pinyin with tone numbers and optional hangul`() {
        val item = ContentItem(
            id = "phrase-ni3-hao3",
            type = ContentItemType.PHRASE,
            source = ContentSource.BUNDLED,
            meaning = "hello",
            audioAssetRef = "audio/phrase-ni3-hao3.wav",
            pinyin = "ni3 hao3",
            targetTones = listOf(3, 3),
        )

        assertEquals("ni3 hao3", item.pinyin)
        assertEquals(listOf(3, 3), item.targetTones)
        assertNull(item.hangul)
    }

    @Test
    fun `attempt and progress hold their sample values`() {
        val attempt = Attempt(
            id = "attempt-2026-09-13-01",
            contentItemId = "phrase-ni3-hao3",
            recordedAt = Instant.parse("2026-09-13T09:12:00Z"),
            userAudioRef = "recordings/attempt-2026-09-13-01.wav",
            transcript = "ni3 hao3",
            feedbackText = "the second syllable needs a deeper dip",
        )
        val progress = Progress(
            contentItemId = attempt.contentItemId,
            timesPracticed = 4,
            lastPracticedAt = attempt.recordedAt,
            feedbackThemes = listOf("third tone", "final -ao"),
        )

        assertEquals(4, progress.timesPracticed)
        assertEquals(listOf("third tone", "final -ao"), progress.feedbackThemes)
    }

    @Test
    fun `field mission holds script turns and personas`() {
        val mission = FieldMission(
            id = "mission-2026-09-13",
            date = LocalDate.parse("2026-09-13"),
            theme = "greetings",
            scriptTurns = listOf(
                ScriptTurn(pinyin = "ni3 hao3", meaning = "hello", targetTones = listOf(3, 3)),
            ),
            source = ContentSource.BUNDLED,
            localPersonaIds = listOf("local-1", "local-2", "local-3", "local-4", "local-5"),
        )

        assertEquals(5, mission.localPersonaIds.size)
        assertEquals(listOf(3, 3), mission.scriptTurns.single().targetTones)
    }
}
