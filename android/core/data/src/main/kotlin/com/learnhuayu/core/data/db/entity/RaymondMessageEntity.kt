package com.learnhuayu.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.learnhuayu.core.model.RaymondMessage
import com.learnhuayu.core.model.RaymondRole
import java.time.Instant

@Entity(tableName = "raymond_messages")
data class RaymondMessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val text: String,
    val audioRef: String?,
    val createdAt: Long,
)

internal fun RaymondMessageEntity.toModel(): RaymondMessage = RaymondMessage(
    id = id,
    role = RaymondRole.valueOf(role),
    text = text,
    audioRef = audioRef,
    createdAt = Instant.ofEpochMilli(createdAt),
)

internal fun RaymondMessage.toEntity(): RaymondMessageEntity = RaymondMessageEntity(
    id = id,
    role = role.name,
    text = text,
    audioRef = audioRef,
    createdAt = createdAt.toEpochMilli(),
)
