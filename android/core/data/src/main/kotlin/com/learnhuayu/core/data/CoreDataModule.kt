package com.learnhuayu.core.data

import android.content.Context
import android.content.res.AssetManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.learnhuayu.core.data.db.LearnHuayuDatabase
import com.learnhuayu.core.data.db.dao.AttemptDao
import com.learnhuayu.core.data.db.dao.ConversationSessionDao
import com.learnhuayu.core.data.db.dao.DebriefEntryDao
import com.learnhuayu.core.data.db.dao.FieldMissionDao
import com.learnhuayu.core.data.db.dao.LocalPersonaDao
import com.learnhuayu.core.data.db.dao.MissionSessionDao
import com.learnhuayu.core.data.db.dao.MissionTurnDao
import com.learnhuayu.core.data.db.dao.ProgressDao
import com.learnhuayu.core.data.db.dao.RaymondMessageDao
import com.learnhuayu.core.data.db.dao.TurnDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CoreDataModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    fun provideAssetManager(@ApplicationContext context: Context): AssetManager = context.assets

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(PREFERENCES_DATASTORE_NAME) },
    )

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LearnHuayuDatabase = Room.databaseBuilder(context, LearnHuayuDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideAttemptDao(database: LearnHuayuDatabase): AttemptDao = database.attemptDao()

    @Provides
    fun provideProgressDao(database: LearnHuayuDatabase): ProgressDao = database.progressDao()

    @Provides
    fun provideConversationSessionDao(database: LearnHuayuDatabase): ConversationSessionDao = database.conversationSessionDao()

    @Provides
    fun provideTurnDao(database: LearnHuayuDatabase): TurnDao = database.turnDao()

    @Provides
    fun provideFieldMissionDao(database: LearnHuayuDatabase): FieldMissionDao = database.fieldMissionDao()

    @Provides
    fun provideLocalPersonaDao(database: LearnHuayuDatabase): LocalPersonaDao = database.localPersonaDao()

    @Provides
    fun provideMissionSessionDao(database: LearnHuayuDatabase): MissionSessionDao = database.missionSessionDao()

    @Provides
    fun provideMissionTurnDao(database: LearnHuayuDatabase): MissionTurnDao = database.missionTurnDao()

    @Provides
    fun provideDebriefEntryDao(database: LearnHuayuDatabase): DebriefEntryDao = database.debriefEntryDao()

    @Provides
    fun provideRaymondMessageDao(database: LearnHuayuDatabase): RaymondMessageDao = database.raymondMessageDao()
}

private const val DATABASE_NAME = "learnhuayu.db"
private const val PREFERENCES_DATASTORE_NAME = "user_preferences"
