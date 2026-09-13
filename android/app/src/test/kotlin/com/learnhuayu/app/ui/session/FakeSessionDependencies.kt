package com.learnhuayu.app.ui.session

import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.data.content.ContentAssetException
import com.learnhuayu.core.data.content.ContentModule
import com.learnhuayu.core.data.prefs.PreferencesRepository
import com.learnhuayu.core.data.prefs.UserPreferences
import com.learnhuayu.core.data.repository.AttemptRepository
import com.learnhuayu.core.data.repository.ProgressRepository
import com.learnhuayu.core.model.Attempt
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.LearningModule
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import com.learnhuayu.core.model.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeBundledContentRepository(
    private val contentModules: List<ContentModule> = emptyList(),
    private val items: Map<String, ContentItem> = emptyMap(),
) : BundledContentRepository {

    override suspend fun modules(): List<ContentModule> = contentModules

    override suspend fun module(moduleId: String): ContentModule? = contentModules.firstOrNull { it.id == moduleId }

    override suspend fun contentItem(contentItemId: String): ContentItem? = items[contentItemId]

    override suspend fun contentItems(contentItemIds: List<String>): List<ContentItem> = contentItemIds.map { id ->
        items[id] ?: throw ContentAssetException("Unknown content item id: $id")
    }

    override fun audioAssetPath(audioAssetRef: String): String = "audio/$audioAssetRef"
}

class FakeLearningModule(
    override val id: String = "tones",
    override val title: String = "Tones",
    private val lessonSpecs: List<LessonSpec> = emptyList(),
    private val practiceSpecs: List<PracticeSpec> = emptyList(),
) : LearningModule {

    override suspend fun lessons(): List<LessonSpec> = lessonSpecs

    override suspend fun practices(): List<PracticeSpec> = practiceSpecs
}

class FakeProgressRepository : ProgressRepository {

    val upserts = mutableListOf<Progress>()

    private val stored = mutableMapOf<String, Progress>()

    override suspend fun upsert(progress: Progress) {
        upserts += progress
        stored[progress.contentItemId] = progress
    }

    override suspend fun byId(contentItemId: String): Progress? = stored[contentItemId]

    override fun all(): Flow<List<Progress>> = flowOf(stored.values.toList())

    override suspend fun deleteById(contentItemId: String) {
        stored.remove(contentItemId)
    }

    fun seed(progress: Progress) {
        stored[progress.contentItemId] = progress
    }
}

class FakeAttemptRepository : AttemptRepository {

    val upserts = mutableListOf<Attempt>()

    override suspend fun upsert(attempt: Attempt) {
        upserts += attempt
    }

    override suspend fun byId(id: String): Attempt? = upserts.firstOrNull { it.id == id }

    override fun byContentItem(contentItemId: String): Flow<List<Attempt>> = flowOf(upserts.filter { it.contentItemId == contentItemId })

    override fun all(): Flow<List<Attempt>> = flowOf(upserts.toList())

    override suspend fun deleteById(id: String) {
        upserts.removeAll { it.id == id }
    }
}

class FakePreferencesRepository(
    initial: UserPreferences = UserPreferences(
        dailyGoalMinutes = null,
        sessionLengthMinutes = null,
        showHangul = false,
        recordingsToProviderConsent = false,
        consentVersion = "0",
    ),
) : PreferencesRepository {

    private val mutablePreferences = MutableStateFlow(initial)

    override val preferences: Flow<UserPreferences> = mutablePreferences

    override suspend fun setDailyGoalMinutes(minutes: Int?) {
        mutablePreferences.value = mutablePreferences.value.copy(dailyGoalMinutes = minutes)
    }

    override suspend fun setSessionLengthMinutes(minutes: Int?) {
        mutablePreferences.value = mutablePreferences.value.copy(sessionLengthMinutes = minutes)
    }

    override suspend fun setShowHangul(enabled: Boolean) {
        mutablePreferences.value = mutablePreferences.value.copy(showHangul = enabled)
    }

    override suspend fun setRecordingsToProviderConsent(granted: Boolean) {
        mutablePreferences.value = mutablePreferences.value.copy(recordingsToProviderConsent = granted)
    }

    override suspend fun setConsentVersion(version: String) {
        mutablePreferences.value = mutablePreferences.value.copy(consentVersion = version)
    }
}
