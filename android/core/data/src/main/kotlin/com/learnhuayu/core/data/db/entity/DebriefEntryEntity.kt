package com.learnhuayu.core.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.learnhuayu.core.model.DebriefEntry
import com.learnhuayu.core.model.DebriefKind
import java.time.Instant

@Entity(
    tableName = "debrief_entries",
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
data class DebriefEntryEntity(
    @PrimaryKey val id: String,
    val missionId: String,
    val pinyin: String,
    val kind: String,
    val note: String?,
    val createdAt: Long,
)

internal fun DebriefEntryEntity.toModel(): DebriefEntry = DebriefEntry(
    id = id,
    missionId = missionId,
    pinyin = pinyin,
    kind = DebriefKind.valueOf(kind),
    note = note,
    createdAt = Instant.ofEpochMilli(createdAt),
)

internal fun DebriefEntry.toEntity(): DebriefEntryEntity = DebriefEntryEntity(
    id = id,
    missionId = missionId,
    pinyin = pinyin,
    kind = kind.name,
    note = note,
    createdAt = createdAt.toEpochMilli(),
)
