package com.learnhuayu.core.data.content

import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultBundledContentRepository @Inject constructor(
    private val source: ContentSource,
    private val json: Json,
) : BundledContentRepository {
    private val lock = Mutex()
    private var corpus: Corpus? = null

    override suspend fun modules(): List<ContentModule> = loadCorpus().modules

    override suspend fun module(moduleId: String): ContentModule? = modules().firstOrNull { module -> module.id == moduleId }

    override suspend fun contentItem(contentItemId: String): ContentItem? = loadCorpus().items[contentItemId]

    override suspend fun contentItems(contentItemIds: List<String>): List<ContentItem> {
        val items = loadCorpus().items
        return contentItemIds.map { contentItemId ->
            items[contentItemId]
                ?: throw ContentAssetException("Unknown content item id: $contentItemId")
        }
    }

    override fun audioAssetPath(audioAssetRef: String): String = AUDIO_ROOT + audioAssetRef

    private suspend fun loadCorpus(): Corpus = lock.withLock {
        corpus ?: readCorpus().also { loaded -> corpus = loaded }
    }

    private fun readCorpus(): Corpus {
        val index = readAsset<ContentIndexDto>(CONTENT_INDEX_PATH)
        val modules = mutableListOf<ContentModule>()
        val items = mutableMapOf<String, ContentItem>()
        index.modules.forEach { entry ->
            val directory = entry.path.substringBeforeLast('/', missingDelimiterValue = "")
            val moduleIndex = readAsset<ModuleIndexDto>(contentPath(entry.index))
            val module = readAsset<ModuleDto>(contentPath(directory, moduleIndex.module))
            val lessons = readAsset<LessonsFileDto>(contentPath(directory, moduleIndex.lessons))
            val itemFile = readAsset<ItemsFileDto>(contentPath(directory, moduleIndex.items))
            if (module.id != entry.id ||
                moduleIndex.moduleId != entry.id ||
                lessons.moduleId != entry.id ||
                itemFile.moduleId != entry.id
            ) {
                throw ContentAssetException("Content manifests disagree on module id: ${entry.id}")
            }
            val lessonSpecs = mutableListOf<LessonSpec>()
            val practiceSpecs = mutableListOf<PracticeSpec>()
            lessons.lessons.forEach { lesson ->
                when (lesson.kind) {
                    LESSON_KIND -> lessonSpecs += lesson.toLessonSpec()
                    PRACTICE_KIND -> practiceSpecs += lesson.toPracticeSpec()
                    else -> throw ContentAssetException("Unknown lesson kind: ${lesson.kind}")
                }
            }
            modules += ContentModule(module.id, module.title, module.theme, lessonSpecs, practiceSpecs)
            itemFile.items.forEach { dto ->
                if (items.put(dto.id, dto.toModel()) != null) {
                    throw ContentAssetException("Duplicate content item id: ${dto.id}")
                }
            }
        }
        return Corpus(modules, items)
    }

    private inline fun <reified T> readAsset(path: String): T {
        val text =
            try {
                source.readText(path)
            } catch (error: IOException) {
                throw ContentAssetException("Cannot read content asset: $path", error)
            }
        return try {
            json.decodeFromString<T>(text)
        } catch (error: SerializationException) {
            throw ContentAssetException("Cannot parse content asset: $path", error)
        }
    }

    private class Corpus(
        val modules: List<ContentModule>,
        val items: Map<String, ContentItem>,
    )
}

private const val CONTENT_INDEX_PATH = "content/index.json"
private const val CONTENT_ROOT = "content/"
private const val AUDIO_ROOT = "audio/"
private const val LESSON_KIND = "lesson"
private const val PRACTICE_KIND = "practice"

private fun contentPath(path: String): String = CONTENT_ROOT + path

private fun contentPath(directory: String, file: String): String = if (directory.isEmpty()) contentPath(file) else "$CONTENT_ROOT$directory/$file"
