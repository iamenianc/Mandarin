package com.learnhuayu.core.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.learnhuayu.core.model.MissionSpeaker
import com.learnhuayu.core.model.MissionTurn

@Entity(
    tableName = "mission_turns",
    foreignKeys = [
        ForeignKey(
            entity = MissionSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class MissionTurnEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val speaker: String,
    val audioRef: String?,
    val transcript: String?,
)

internal fun MissionTurnEntity.toModel(): MissionTurn = MissionTurn(
    id = id,
    sessionId = sessionId,
    speaker = MissionSpeaker.valueOf(speaker),
    audioRef = audioRef,
    transcript = transcript,
)

internal fun MissionTurn.toEntity(): MissionTurnEntity = MissionTurnEntity(
    id = id,
    sessionId = sessionId,
    speaker = speaker.name,
    audioRef = audioRef,
    transcript = transcript,
)
