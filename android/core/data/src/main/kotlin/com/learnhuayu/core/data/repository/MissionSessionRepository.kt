package com.learnhuayu.core.data.repository

import com.learnhuayu.core.data.db.dao.MissionSessionDao
import com.learnhuayu.core.data.db.entity.MissionSessionEntity
import com.learnhuayu.core.data.db.entity.toEntity
import com.learnhuayu.core.data.db.entity.toModel
import com.learnhuayu.core.model.MissionSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface MissionSessionRepository {
    suspend fun upsert(session: MissionSession)

    suspend fun byId(id: String): MissionSession?

    fun byMission(missionId: String): Flow<List<MissionSession>>

    suspend fun deleteById(id: String)
}

internal class RoomMissionSessionRepository @Inject constructor(
    private val dao: MissionSessionDao,
) : MissionSessionRepository {
    override suspend fun upsert(session: MissionSession) = dao.upsert(session.toEntity())

    override suspend fun byId(id: String): MissionSession? = dao.byId(id)?.toModel()

    override fun byMission(missionId: String): Flow<List<MissionSession>> = dao.byMission(missionId).map { sessions -> sessions.map(MissionSessionEntity::toModel) }

    override suspend fun deleteById(id: String) = dao.deleteById(id)
}
