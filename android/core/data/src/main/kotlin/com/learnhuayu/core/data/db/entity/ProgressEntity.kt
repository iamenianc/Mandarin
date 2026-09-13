package com.learnhuayu.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.learnhuayu.core.model.Progress
import java.time.Instant

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val contentItemId: String,
    val timesPracticed: Int,
    val lastPracticedAt: Long?,
    val feedbackThemesJson: String,
)

internal fun ProgressEntity.toModel(): Progress = Progress(
    contentItemId = contentItemId,
    timesPracticed = timesPracticed,
    lastPracticedAt = lastPracticedAt?.let(Instant::ofEpochMilli),
    feedbackThemes = decodeStringList(feedbackThemesJson),
)

internal fun Progress.toEntity(): ProgressEntity = ProgressEntity(
    contentItemId = contentItemId,
    timesPracticed = timesPracticed,
    lastPracticedAt = lastPracticedAt?.toEpochMilli(),
    feedbackThemesJson = encodeStringList(feedbackThemes),
)
