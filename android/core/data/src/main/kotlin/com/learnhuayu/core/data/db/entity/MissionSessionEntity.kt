package com.learnhuayu.core.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.learnhuayu.core.model.MissionSession
import java.time.Instant

@Entity(
    tableName = "mission_sessions",
    foreignKeys = [
        ForeignKey(
            entity = FieldMissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["missionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("missionId")],
)
data class MissionSessionEntity(
    @PrimaryKey val id: String,
    val missionId: String,
    val personaId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val turnCount: Int,
    val outcome: String?,
)

internal fun MissionSessionEntity.toModel(): MissionSession = MissionSession(
    id = id,
    missionId = missionId,
    personaId = personaId,
    startedAt = Instant.ofEpochMilli(startedAt),
    endedAt = endedAt?.let(Instant::ofEpochMilli),
    turnCount = turnCount,
    outcome = outcome,
)

internal fun MissionSession.toEntity(): MissionSessionEntity = MissionSessionEntity(
    id = id,
    missionId = missionId,
    personaId = personaId,
    startedAt = startedAt.toEpochMilli(),
    endedAt = endedAt?.toEpochMilli(),
    turnCount = turnCount,
    outcome = outcome,
)
