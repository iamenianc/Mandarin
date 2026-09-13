package com.learnhuayu.core.data.repository

import com.learnhuayu.core.data.db.dao.AttemptDao
import com.learnhuayu.core.data.db.entity.AttemptEntity
import com.learnhuayu.core.data.db.entity.toEntity
import com.learnhuayu.core.data.db.entity.toModel
import com.learnhuayu.core.model.Attempt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface AttemptRepository {
    suspend fun upsert(attempt: Attempt)

    suspend fun byId(id: String): Attempt?

    fun byContentItem(contentItemId: String): Flow<List<Attempt>>

    fun all(): Flow<List<Attempt>>

    suspend fun deleteById(id: String)
}

internal class RoomAttemptRepository @Inject constructor(
    private val dao: AttemptDao,
) : AttemptRepository {
    override suspend fun upsert(attempt: Attempt) = dao.upsert(attempt.toEntity())

    override suspend fun byId(id: String): Attempt? = dao.byId(id)?.toModel()

    override fun byContentItem(contentItemId: String): Flow<List<Attempt>> = dao.byContentItem(contentItemId).map { attempts -> attempts.map(AttemptEntity::toModel) }

    override fun all(): Flow<List<Attempt>> = dao.all().map { attempts -> attempts.map(AttemptEntity::toModel) }

    override suspend fun deleteById(id: String) = dao.deleteById(id)
}
