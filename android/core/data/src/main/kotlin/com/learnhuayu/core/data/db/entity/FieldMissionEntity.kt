package com.learnhuayu.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.FieldMission
import java.time.LocalDate

@Entity(tableName = "field_missions")
data class FieldMissionEntity(
    @PrimaryKey val id: String,
    val date: String,
    val theme: String,
    val scriptTurnsJson: String,
    val source: String,
    val localPersonaIdsJson: String,
)

internal fun FieldMissionEntity.toModel(): FieldMission = FieldMission(
    id = id,
    date = LocalDate.parse(date),
    theme = theme,
    scriptTurns = decodeScriptTurns(scriptTurnsJson),
    source = ContentSource.valueOf(source),
    localPersonaIds = decodeStringList(localPersonaIdsJson),
)

internal fun FieldMission.toEntity(): FieldMissionEntity = FieldMissionEntity(
    id = id,
    date = date.toString(),
    theme = theme,
    scriptTurnsJson = encodeScriptTurns(scriptTurns),
    source = source.name,
    localPersonaIdsJson = encodeStringList(localPersonaIds),
)
