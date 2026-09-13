package com.learnhuayu.core.data.repository

import com.learnhuayu.core.data.db.dao.ConversationSessionDao
import com.learnhuayu.core.data.db.entity.ConversationSessionEntity
import com.learnhuayu.core.data.db.entity.toEntity
import com.learnhuayu.core.data.db.entity.toModel
import com.learnhuayu.core.model.ConversationSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ConversationSessionRepository {
    suspend fun upsert(session: ConversationSession)

    suspend fun byId(id: String): ConversationSession?

    fun all(): Flow<List<ConversationSession>>

    suspend fun deleteById(id: String)
}

internal class RoomConversationSessionRepository @Inject constructor(
    private val dao: ConversationSessionDao,
) : ConversationSessionRepository {
    override suspend fun upsert(session: ConversationSession) = dao.upsert(session.toEntity())

    override suspend fun byId(id: String): ConversationSession? = dao.byId(id)?.toModel()

    override fun all(): Flow<List<ConversationSession>> = dao.all().map { sessions -> sessions.map(ConversationSessionEntity::toModel) }

    override suspend fun deleteById(id: String) = dao.deleteById(id)
}
