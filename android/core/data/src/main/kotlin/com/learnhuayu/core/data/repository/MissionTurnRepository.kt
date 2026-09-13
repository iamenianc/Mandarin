package com.learnhuayu.core.data.repository

import com.learnhuayu.core.data.db.dao.MissionTurnDao
import com.learnhuayu.core.data.db.entity.MissionTurnEntity
import com.learnhuayu.core.data.db.entity.toEntity
import com.learnhuayu.core.data.db.entity.toModel
import com.learnhuayu.core.model.MissionTurn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface MissionTurnRepository {
    suspend fun upsert(turn: MissionTurn)

    suspend fun byId(id: String): MissionTurn?

    fun bySession(sessionId: String): Flow<List<MissionTurn>>

    suspend fun deleteById(id: String)
}

internal class RoomMissionTurnRepository @Inject constructor(
    private val dao: MissionTurnDao,
) : MissionTurnRepository {
    override suspend fun upsert(turn: MissionTurn) = dao.upsert(turn.toEntity())

    override suspend fun byId(id: String): MissionTurn? = dao.byId(id)?.toModel()

    override fun bySession(sessionId: String): Flow<List<MissionTurn>> = dao.bySession(sessionId).map { turns -> turns.map(MissionTurnEntity::toModel) }

    override suspend fun deleteById(id: String) = dao.deleteById(id)
}
