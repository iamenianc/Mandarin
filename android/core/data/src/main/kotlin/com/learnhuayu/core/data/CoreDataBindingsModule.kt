package com.learnhuayu.core.data

import com.learnhuayu.core.data.content.AssetManagerContentSource
import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.data.content.ContentSource
import com.learnhuayu.core.data.content.DefaultBundledContentRepository
import com.learnhuayu.core.data.prefs.BundledPreferencesDefaultsSource
import com.learnhuayu.core.data.prefs.DataStorePreferencesRepository
import com.learnhuayu.core.data.prefs.PreferencesDefaultsSource
import com.learnhuayu.core.data.prefs.PreferencesRepository
import com.learnhuayu.core.data.repository.AttemptRepository
import com.learnhuayu.core.data.repository.ConversationSessionRepository
import com.learnhuayu.core.data.repository.DebriefEntryRepository
import com.learnhuayu.core.data.repository.FieldMissionRepository
import com.learnhuayu.core.data.repository.LocalPersonaRepository
import com.learnhuayu.core.data.repository.MissionSessionRepository
import com.learnhuayu.core.data.repository.MissionTurnRepository
import com.learnhuayu.core.data.repository.ProgressRepository
import com.learnhuayu.core.data.repository.RaymondMessageRepository
import com.learnhuayu.core.data.repository.RoomAttemptRepository
import com.learnhuayu.core.data.repository.RoomConversationSessionRepository
import com.learnhuayu.core.data.repository.RoomDebriefEntryRepository
import com.learnhuayu.core.data.repository.RoomFieldMissionRepository
import com.learnhuayu.core.data.repository.RoomLocalPersonaRepository
import com.learnhuayu.core.data.repository.RoomMissionSessionRepository
import com.learnhuayu.core.data.repository.RoomMissionTurnRepository
import com.learnhuayu.core.data.repository.RoomProgressRepository
import com.learnhuayu.core.data.repository.RoomRaymondMessageRepository
import com.learnhuayu.core.data.repository.RoomTurnRepository
import com.learnhuayu.core.data.repository.TurnRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CoreDataBindingsModule {
    @Binds
    abstract fun bindContentSource(impl: AssetManagerContentSource): ContentSource

    @Binds
    abstract fun bindBundledContentRepository(impl: DefaultBundledContentRepository): BundledContentRepository

    @Binds
    abstract fun bindPreferencesDefaultsSource(impl: BundledPreferencesDefaultsSource): PreferencesDefaultsSource

    @Binds
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository

    @Binds
    abstract fun bindAttemptRepository(impl: RoomAttemptRepository): AttemptRepository

    @Binds
    abstract fun bindProgressRepository(impl: RoomProgressRepository): ProgressRepository

    @Binds
    abstract fun bindConversationSessionRepository(
        impl: RoomConversationSessionRepository,
    ): ConversationSessionRepository

    @Binds
    abstract fun bindTurnRepository(impl: RoomTurnRepository): TurnRepository

    @Binds
    abstract fun bindFieldMissionRepository(impl: RoomFieldMissionRepository): FieldMissionRepository

    @Binds
    abstract fun bindLocalPersonaRepository(impl: RoomLocalPersonaRepository): LocalPersonaRepository

    @Binds
    abstract fun bindMissionSessionRepository(impl: RoomMissionSessionRepository): MissionSessionRepository

    @Binds
    abstract fun bindMissionTurnRepository(impl: RoomMissionTurnRepository): MissionTurnRepository

    @Binds
    abstract fun bindDebriefEntryRepository(impl: RoomDebriefEntryRepository): DebriefEntryRepository

    @Binds
    abstract fun bindRaymondMessageRepository(impl: RoomRaymondMessageRepository): RaymondMessageRepository
}
