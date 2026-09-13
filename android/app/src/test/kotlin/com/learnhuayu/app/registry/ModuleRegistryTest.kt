package com.learnhuayu.app.registry

import com.learnhuayu.core.model.LearningModule
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ModuleRegistryTest {

    private class FakeModule(
        override val id: String,
        override val title: String,
    ) : LearningModule {
        override fun lessons(): List<LessonSpec> = emptyList()

        override fun practices(): List<PracticeSpec> = emptyList()
    }

    @Test
    fun `modules are ordered with the preferred order first`() {
        val registry = ModuleRegistry(
            setOf(
                FakeModule("vocabulary", "Vocabulary"),
                FakeModule("tones", "Tones"),
                FakeModule("listening", "Listening"),
            ),
        )

        assertEquals(listOf("tones", "vocabulary", "listening"), registry.modules.map { it.id })
    }

    @Test
    fun `unknown modules follow the preferred order sorted by title`() {
        val registry = ModuleRegistry(
            setOf(
                FakeModule("zonk", "Zonk"),
                FakeModule("alpha", "Alpha"),
                FakeModule("tones", "Tones"),
            ),
        )

        assertEquals(listOf("tones", "alpha", "zonk"), registry.modules.map { it.id })
    }

    @Test
    fun `lookup finds a module by id`() {
        val tones = FakeModule("tones", "Tones")
        val registry = ModuleRegistry(setOf(tones))

        assertEquals(tones, registry.module("tones"))
        assertNull(registry.module("missing"))
    }

    @Test
    fun `duplicate ids are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ModuleRegistry(
                setOf(
                    FakeModule("tones", "Tones"),
                    FakeModule("tones", "More tones"),
                ),
            )
        }
    }
}
