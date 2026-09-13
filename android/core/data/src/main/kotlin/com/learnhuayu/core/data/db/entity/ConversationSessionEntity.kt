package com.learnhuayu.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.learnhuayu.core.model.ConversationSession
import java.time.Instant

@Entity(tableName = "conversation_sessions")
data class ConversationSessionEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val turnCount: Int,
    val summary: String?,
)

internal fun ConversationSessionEntity.toModel(): ConversationSession = ConversationSession(
    id = id,
    lessonId = lessonId,
    startedAt = Instant.ofEpochMilli(startedAt),
    endedAt = endedAt?.let(Instant::ofEpochMilli),
    turnCount = turnCount,
    summary = summary,
)

internal fun ConversationSession.toEntity(): ConversationSessionEntity = ConversationSessionEntity(
    id = id,
    lessonId = lessonId,
    startedAt = startedAt.toEpochMilli(),
    endedAt = endedAt?.toEpochMilli(),
    turnCount = turnCount,
    summary = summary,
)
