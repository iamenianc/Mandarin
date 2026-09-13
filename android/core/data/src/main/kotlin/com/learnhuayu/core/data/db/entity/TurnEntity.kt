package com.learnhuayu.core.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.learnhuayu.core.model.ConversationSpeaker
import com.learnhuayu.core.model.Turn

@Entity(
    tableName = "turns",
    foreignKeys = [
        ForeignKey(
            entity = ConversationSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class TurnEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val speaker: String,
    val audioRef: String?,
    val transcript: String?,
    val feedback: String?,
)

internal fun TurnEntity.toModel(): Turn = Turn(
    id = id,
    sessionId = sessionId,
    speaker = ConversationSpeaker.valueOf(speaker),
    audioRef = audioRef,
    transcript = transcript,
    feedback = feedback,
)

internal fun Turn.toEntity(): TurnEntity = TurnEntity(
    id = id,
    sessionId = sessionId,
    speaker = speaker.name,
    audioRef = audioRef,
    transcript = transcript,
    feedback = feedback,
)
