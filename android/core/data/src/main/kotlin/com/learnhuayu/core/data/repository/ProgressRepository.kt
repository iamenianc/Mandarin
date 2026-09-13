package com.learnhuayu.core.data.repository

import com.learnhuayu.core.data.db.dao.ProgressDao
import com.learnhuayu.core.data.db.entity.ProgressEntity
import com.learnhuayu.core.data.db.entity.toEntity
import com.learnhuayu.core.data.db.entity.toModel
import com.learnhuayu.core.model.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ProgressRepository {
    suspend fun upsert(progress: Progress)

    suspend fun byId(contentItemId: String): Progress?

    fun all(): Flow<List<Progress>>

    suspend fun deleteById(contentItemId: String)
}

internal class RoomProgressRepository @Inject constructor(
    private val dao: ProgressDao,
) : ProgressRepository {
    override suspend fun upsert(progress: Progress) = dao.upsert(progress.toEntity())

    override suspend fun byId(contentItemId: String): Progress? = dao.byId(contentItemId)?.toModel()

    override fun all(): Flow<List<Progress>> = dao.all().map { entries -> entries.map(ProgressEntity::toModel) }

    override suspend fun deleteById(contentItemId: String) = dao.deleteById(contentItemId)
}
