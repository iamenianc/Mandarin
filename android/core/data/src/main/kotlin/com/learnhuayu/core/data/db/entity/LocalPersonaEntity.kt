package com.learnhuayu.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.learnhuayu.core.model.LocalPersona

@Entity(tableName = "local_personas")
data class LocalPersonaEntity(
    @PrimaryKey val id: String,
    val label: String,
    val settingRole: String,
    val personality: String,
    val voiceProfile: String,
    val pace: String,
)

internal fun LocalPersonaEntity.toModel(): LocalPersona = LocalPersona(
    id = id,
    label = label,
    settingRole = settingRole,
    personality = personality,
    voiceProfile = voiceProfile,
    pace = pace,
)

internal fun LocalPersona.toEntity(): LocalPersonaEntity = LocalPersonaEntity(
    id = id,
    label = label,
    settingRole = settingRole,
    personality = personality,
    voiceProfile = voiceProfile,
    pace = pace,
)
