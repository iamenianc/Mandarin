package com.learnhuayu.core.data.repository

import com.learnhuayu.core.data.db.dao.DebriefEntryDao
import com.learnhuayu.core.data.db.entity.DebriefEntryEntity
import com.learnhuayu.core.data.db.entity.toEntity
import com.learnhuayu.core.data.db.entity.toModel
import com.learnhuayu.core.model.DebriefEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface DebriefEntryRepository {
    suspend fun upsert(entry: DebriefEntry)

    suspend fun byId(id: String): DebriefEntry?

    fun byMission(missionId: String): Flow<List<DebriefEntry>>

    fun all(): Flow<List<DebriefEntry>>

    suspend fun deleteById(id: String)
}

internal class RoomDebriefEntryRepository @Inject constructor(
    private val dao: DebriefEntryDao,
) : DebriefEntryRepository {
    override suspend fun upsert(entry: DebriefEntry) = dao.upsert(entry.toEntity())

    override suspend fun byId(id: String): DebriefEntry? = dao.byId(id)?.toModel()

    override fun byMission(missionId: String): Flow<List<DebriefEntry>> = dao.byMission(missionId).map { entries -> entries.map(DebriefEntryEntity::toModel) }

    override fun all(): Flow<List<DebriefEntry>> = dao.all().map { entries -> entries.map(DebriefEntryEntity::toModel) }

    override suspend fun deleteById(id: String) = dao.deleteById(id)
}
