package com.learnhuayu.core.data.deletion

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.learnhuayu.core.data.db.LearnHuayuDatabase
import com.learnhuayu.core.data.recordings.LocalRecordings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-tap deletion of all local learner data (NFR-4): every Room table, the DataStore
 * preferences, and the app-local microphone recordings. A failure propagates to the
 * caller so the UI can report it instead of pretending success.
 */
interface DataDeletionService {
    suspend fun deleteAll()
}

@Singleton
internal class DefaultDataDeletionService @Inject constructor(
    private val database: LearnHuayuDatabase,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) : DataDeletionService {

    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            dataStore.edit { stored -> stored.clear() }
            LocalRecordings.directory(context.cacheDir).deleteRecursively()
        }
    }
}
