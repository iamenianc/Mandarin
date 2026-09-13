package com.learnhuayu.core.data.repository

import com.learnhuayu.core.data.db.dao.FieldMissionDao
import com.learnhuayu.core.data.db.entity.FieldMissionEntity
import com.learnhuayu.core.data.db.entity.toEntity
import com.learnhuayu.core.data.db.entity.toModel
import com.learnhuayu.core.model.FieldMission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface FieldMissionRepository {
    suspend fun upsert(mission: FieldMission)

    suspend fun byId(id: String): FieldMission?

    fun all(): Flow<List<FieldMission>>

    suspend fun deleteById(id: String)
}

internal class RoomFieldMissionRepository @Inject constructor(
    private val dao: FieldMissionDao,
) : FieldMissionRepository {
    override suspend fun upsert(mission: FieldMission) = dao.upsert(mission.toEntity())

    override suspend fun byId(id: String): FieldMission? = dao.byId(id)?.toModel()

    override fun all(): Flow<List<FieldMission>> = dao.all().map { missions -> missions.map(FieldMissionEntity::toModel) }

    override suspend fun deleteById(id: String) = dao.deleteById(id)
}
