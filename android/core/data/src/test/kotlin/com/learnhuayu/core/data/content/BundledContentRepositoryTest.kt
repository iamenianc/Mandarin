package com.learnhuayu.core.data.content

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class BundledContentRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun repository(vararg files: Pair<String, String>): DefaultBundledContentRepository = DefaultBundledContentRepository(FakeContentSource(files.toMap()), json)

    private fun repository(files: Map<String, String>): DefaultBundledContentRepository = DefaultBundledContentRepository(FakeContentSource(files), json)

    private fun validRepository(): DefaultBundledContentRepository = repository(*validCorpus())

    @Test
    fun modulesMapCorpusToCoreModel() = runTest {
        val modules = validRepository().modules()

        assertThat(modules).hasSize(1)
        val module = modules.single()
        assertThat(module.id).isEqualTo("tones")
        assertThat(module.title).isEqualTo("Tones")
        assertThat(module.theme).isEqualTo("The four tones and the neutral tone.")
        assertThat(module.lessonSpecs).hasSize(1)
        assertThat(module.practiceSpecs).hasSize(1)

        val lesson = module.lessonSpecs.single()
        assertThat(lesson.id).isEqualTo("tones-01-first-tone")
        assertThat(lesson.moduleId).isEqualTo("tones")
        assertThat(lesson.title).isEqualTo("First tone")
        assertThat(lesson.level).isEqualTo("beginner")
        assertThat(lesson.topic).isEqualTo("tone contours")
        assertThat(lesson.contentItemIds).containsExactly("tones-ma1", "tones-ma2").inOrder()

        val practice = module.practiceSpecs.single()
        assertThat(practice.id).isEqualTo("tones-practice-hear-and-name")
        assertThat(practice.moduleId).isEqualTo("tones")
        assertThat(practice.contentItemIds).containsExactly("tones-ma1", "tones-ma2").inOrder()

        assertThat(module.lessons()).isEqualTo(module.lessonSpecs)
        assertThat(module.practices()).isEqualTo(module.practiceSpecs)
    }

    @Test
    fun moduleLookupById() = runTest {
        val repository = validRepository()

        assertThat(repository.module("tones")?.title).isEqualTo("Tones")
        assertThat(repository.module("missing")).isNull()
    }

    @Test
    fun contentItemsMapCorpusFieldsToCoreModel() = runTest {
        val item = validRepository().contentItem("tones-ma1")

        assertThat(item).isNotNull()
        val present = checkNotNull(item)
        assertThat(present.id).isEqualTo("tones-ma1")
        assertThat(present.type).isEqualTo(ContentItemType.WORD)
        assertThat(present.source).isEqualTo(ContentSource.BUNDLED)
        assertThat(present.meaning).isEqualTo("mother (first tone)")
        assertThat(present.pinyin).isEqualTo("ma1")
        assertThat(present.targetTones).containsExactly(1)
        assertThat(present.audioAssetRef).isEqualTo("reference/tones/tones-ma1.ogg")
        assertThat(present.hangul).isEqualTo("엄마")
    }

    @Test
    fun phraseItemWithoutHangulKeepsNullAid() = runTest {
        val item = validRepository().contentItem("tones-ma2")

        assertThat(item).isNotNull()
        val present = checkNotNull(item)
        assertThat(present.type).isEqualTo(ContentItemType.PHRASE)
        assertThat(present.hangul).isNull()
    }

    @Test
    fun contentItemsPreserveRequestedOrder() = runTest {
        val items = validRepository().contentItems(listOf("tones-ma2", "tones-ma1"))

        assertThat(items.map { item -> item.id }).containsExactly("tones-ma2", "tones-ma1").inOrder()
    }

    @Test
    fun unknownContentItemIsRejected() = runTest {
        val repository = validRepository()

        assertThat(repository.contentItem("tones-ma9")).isNull()
        val failure = runCatching { repository.contentItems(listOf("tones-ma9")) }.exceptionOrNull()
        assertThat(failure).isInstanceOf(ContentAssetException::class.java)
    }

    @Test
    fun audioAssetPathResolvesUnderApkAudioRoot() {
        val repository = repository()

        assertThat(repository.audioAssetPath("reference/tones/tones-ma1.wav"))
            .isEqualTo("audio/reference/tones/tones-ma1.wav")
    }

    @Test
    fun missingAssetIsReported() = runTest {
        val failure =
            runCatching { repository("content/index.json" to contentIndex).modules() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ContentAssetException::class.java)
    }

    @Test
    fun malformedJsonIsReported() = runTest {
        val files = validCorpusFiles()
        files["content/tones/items.json"] = "{ not json"

        val failure = runCatching { repository(files).modules() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ContentAssetException::class.java)
    }

    @Test
    fun mismatchedModuleIdIsReported() = runTest {
        val files = validCorpusFiles()
        files["content/tones/module.json"] = moduleJson.replace("\"id\": \"tones\"", "\"id\": \"vocabulary\"")

        val failure = runCatching { repository(files).modules() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ContentAssetException::class.java)
    }

    @Test
    fun duplicateItemIdsAreRejected() = runTest {
        val files = validCorpusFiles()
        files["content/tones/items.json"] =
            itemsJson.replace("\"id\": \"tones-ma2\"", "\"id\": \"tones-ma1\"")

        val failure = runCatching { repository(files).modules() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ContentAssetException::class.java)
    }

    @Test
    fun unknownItemTypeIsRejected() = runTest {
        val files = validCorpusFiles()
        files["content/tones/items.json"] =
            itemsJson.replace("\"type\": \"word\"", "\"type\": \"sentence\"")

        val failure = runCatching { repository(files).modules() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ContentAssetException::class.java)
    }

    @Test
    fun unknownLessonKindIsRejected() = runTest {
        val files = validCorpusFiles()
        files["content/tones/lessons.json"] =
            lessonsJson.replace("\"kind\": \"practice\"", "\"kind\": \"quiz\"")

        val failure = runCatching { repository(files).modules() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ContentAssetException::class.java)
    }

    private fun validCorpusFiles(): MutableMap<String, String> = mutableMapOf(
        "content/index.json" to contentIndex,
        "content/tones/index.json" to moduleIndex,
        "content/tones/module.json" to moduleJson,
        "content/tones/lessons.json" to lessonsJson,
        "content/tones/items.json" to itemsJson,
    )

    private fun validCorpus(): Array<Pair<String, String>> = validCorpusFiles().map { entry -> entry.key to entry.value }.toTypedArray()
}

private val contentIndex =
    """
    {
      "version": 1,
      "schema": {
        "module": "schema/module.schema.json",
        "lesson": "schema/lesson.schema.json",
        "contentItem": "schema/content-item.schema.json"
      },
      "modules": [
        {
          "id": "tones",
          "title": "Tones",
          "path": "tones/module.json",
          "index": "tones/index.json",
          "lessonCount": 2,
          "itemCount": 2
        }
      ]
    }
    """.trimIndent()

private val moduleIndex =
    """
    {
      "version": 1,
      "moduleId": "tones",
      "module": "module.json",
      "lessons": "lessons.json",
      "items": "items.json",
      "lessonCount": 2,
      "itemCount": 2
    }
    """.trimIndent()

private val moduleJson =
    """
    {
      "id": "tones",
      "title": "Tones",
      "theme": "The four tones and the neutral tone.",
      "lessonIds": ["tones-01-first-tone", "tones-practice-hear-and-name"]
    }
    """.trimIndent()

private val lessonsJson =
    """
    {
      "moduleId": "tones",
      "lessons": [
        {
          "id": "tones-01-first-tone",
          "moduleId": "tones",
          "title": "First tone",
          "level": "beginner",
          "topic": "tone contours",
          "kind": "lesson",
          "contentItemIds": ["tones-ma1", "tones-ma2"]
        },
        {
          "id": "tones-practice-hear-and-name",
          "moduleId": "tones",
          "title": "Hear and name the tone",
          "level": "beginner",
          "topic": "tone discrimination",
          "kind": "practice",
          "contentItemIds": ["tones-ma1", "tones-ma2"]
        }
      ]
    }
    """.trimIndent()

private val itemsJson =
    """
    {
      "moduleId": "tones",
      "items": [
        {
          "id": "tones-ma1",
          "type": "word",
          "source": "bundled",
          "meaning": "mother (first tone)",
          "pinyin": "ma1",
          "targetTones": [1],
          "audioAssetRef": "reference/tones/tones-ma1.ogg",
          "hangul": "엄마"
        },
        {
          "id": "tones-ma2",
          "type": "phrase",
          "source": "bundled",
          "meaning": "hemp",
          "pinyin": "ma2",
          "targetTones": [2],
          "audioAssetRef": "reference/tones/tones-ma2.ogg"
        }
      ]
    }
    """.trimIndent()
