package com.learnhuayu.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.learnhuayu.core.data.db.entity.AttemptEntity
import com.learnhuayu.core.data.db.entity.ConversationSessionEntity
import com.learnhuayu.core.data.db.entity.DebriefEntryEntity
import com.learnhuayu.core.data.db.entity.FieldMissionEntity
import com.learnhuayu.core.data.db.entity.LocalPersonaEntity
import com.learnhuayu.core.data.db.entity.MissionSessionEntity
import com.learnhuayu.core.data.db.entity.MissionTurnEntity
import com.learnhuayu.core.data.db.entity.ProgressEntity
import com.learnhuayu.core.data.db.entity.RaymondMessageEntity
import com.learnhuayu.core.data.db.entity.TurnEntity

@Database(
    entities = [
        AttemptEntity::class,
        ProgressEntity::class,
        ConversationSessionEntity::class,
        TurnEntity::class,
        FieldMissionEntity::class,
        LocalPersonaEntity::class,
        MissionSessionEntity::class,
        MissionTurnEntity::class,
        DebriefEntryEntity::class,
        RaymondMessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LearnHuayuDatabase : RoomDatabase() {
    abstract fun attemptDao(): AttemptDao

    abstract fun progressDao(): ProgressDao

    abstract fun conversationSessionDao(): ConversationSessionDao

    abstract fun turnDao(): TurnDao

    abstract fun fieldMissionDao(): FieldMissionDao

    abstract fun localPersonaDao(): LocalPersonaDao

    abstract fun missionSessionDao(): MissionSessionDao

    abstract fun missionTurnDao(): MissionTurnDao

    abstract fun debriefEntryDao(): DebriefEntryDao

    abstract fun raymondMessageDao(): RaymondMessageDao
}
