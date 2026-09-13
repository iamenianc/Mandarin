package com.learnhuayu.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.learnhuayu.core.model.Attempt
import java.time.Instant

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey val id: String,
    val contentItemId: String,
    val recordedAt: Long,
    val userAudioRef: String,
    val transcript: String?,
    val feedbackText: String?,
)

internal fun AttemptEntity.toModel(): Attempt = Attempt(
    id = id,
    contentItemId = contentItemId,
    recordedAt = Instant.ofEpochMilli(recordedAt),
    userAudioRef = userAudioRef,
    transcript = transcript,
    feedbackText = feedbackText,
)

internal fun Attempt.toEntity(): AttemptEntity = AttemptEntity(
    id = id,
    contentItemId = contentItemId,
    recordedAt = recordedAt.toEpochMilli(),
    userAudioRef = userAudioRef,
    transcript = transcript,
    feedbackText = feedbackText,
)
