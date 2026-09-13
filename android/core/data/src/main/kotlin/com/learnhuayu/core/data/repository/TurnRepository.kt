package com.learnhuayu.core.data.repository

import com.learnhuayu.core.data.db.dao.TurnDao
import com.learnhuayu.core.data.db.entity.TurnEntity
import com.learnhuayu.core.data.db.entity.toEntity
import com.learnhuayu.core.data.db.entity.toModel
import com.learnhuayu.core.model.Turn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface TurnRepository {
    suspend fun upsert(turn: Turn)

    suspend fun byId(id: String): Turn?

    fun bySession(sessionId: String): Flow<List<Turn>>

    suspend fun deleteById(id: String)
}

internal class RoomTurnRepository @Inject constructor(
    private val dao: TurnDao,
) : TurnRepository {
    override suspend fun upsert(turn: Turn) = dao.upsert(turn.toEntity())

    override suspend fun byId(id: String): Turn? = dao.byId(id)?.toModel()

    override fun bySession(sessionId: String): Flow<List<Turn>> = dao.bySession(sessionId).map { turns -> turns.map(TurnEntity::toModel) }

    override suspend fun deleteById(id: String) = dao.deleteById(id)
}
