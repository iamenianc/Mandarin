package com.learnhuayu.core.data.deletion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.data.db.LearnHuayuDatabase
import com.learnhuayu.core.data.db.entity.ProgressEntity
import com.learnhuayu.core.data.recordings.LocalRecordings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultDataDeletionServiceTest {

    private lateinit var database: LearnHuayuDatabase
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, LearnHuayuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = {
                File(context.cacheDir, "deletion-preferences.preferences_pb").apply { deleteOnExit() }
            },
        )
    }

    @After
    fun tearDown() {
        database.close()
        dataStoreScope.cancel()
    }

    @Test
    fun `deleteAll clears tables, resets preferences, and removes recordings`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        database.progressDao().upsert(
            ProgressEntity(
                contentItemId = "tones-ma1",
                timesPracticed = 3,
                lastPracticedAt = 1_726_000_000_000,
                feedbackThemesJson = """["tone 1"]""",
            ),
        )
        dataStore.edit { stored -> stored[booleanPreferencesKey("showHangul")] = true }
        val recording = LocalRecordings.file(context.cacheDir, "probe.wav")
        recording.parentFile?.mkdirs()
        recording.writeText("pcm")

        val service = DefaultDataDeletionService(database, dataStore, context)
        service.deleteAll()

        assertThat(database.progressDao().all().first()).isEmpty()
        assertThat(dataStore.data.first().asMap()).isEmpty()
        assertThat(recording.exists()).isFalse()
    }
}
