package com.learnhuayu.core.data.repository

import com.learnhuayu.core.data.db.dao.RaymondMessageDao
import com.learnhuayu.core.data.db.entity.RaymondMessageEntity
import com.learnhuayu.core.data.db.entity.toEntity
import com.learnhuayu.core.data.db.entity.toModel
import com.learnhuayu.core.model.RaymondMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface RaymondMessageRepository {
    suspend fun upsert(message: RaymondMessage)

    suspend fun byId(id: String): RaymondMessage?

    fun all(): Flow<List<RaymondMessage>>

    suspend fun deleteById(id: String)
}

internal class RoomRaymondMessageRepository @Inject constructor(
    private val dao: RaymondMessageDao,
) : RaymondMessageRepository {
    override suspend fun upsert(message: RaymondMessage) = dao.upsert(message.toEntity())

    override suspend fun byId(id: String): RaymondMessage? = dao.byId(id)?.toModel()

    override fun all(): Flow<List<RaymondMessage>> = dao.all().map { messages -> messages.map(RaymondMessageEntity::toModel) }

    override suspend fun deleteById(id: String) = dao.deleteById(id)
}
