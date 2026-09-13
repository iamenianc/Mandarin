package com.learnhuayu.core.data.repository

import com.learnhuayu.core.data.db.dao.LocalPersonaDao
import com.learnhuayu.core.data.db.entity.LocalPersonaEntity
import com.learnhuayu.core.data.db.entity.toEntity
import com.learnhuayu.core.data.db.entity.toModel
import com.learnhuayu.core.model.LocalPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface LocalPersonaRepository {
    suspend fun upsert(persona: LocalPersona)

    suspend fun byId(id: String): LocalPersona?

    fun all(): Flow<List<LocalPersona>>

    suspend fun deleteById(id: String)
}

internal class RoomLocalPersonaRepository @Inject constructor(
    private val dao: LocalPersonaDao,
) : LocalPersonaRepository {
    override suspend fun upsert(persona: LocalPersona) = dao.upsert(persona.toEntity())

    override suspend fun byId(id: String): LocalPersona? = dao.byId(id)?.toModel()

    override fun all(): Flow<List<LocalPersona>> = dao.all().map { personas -> personas.map(LocalPersonaEntity::toModel) }

    override suspend fun deleteById(id: String) = dao.deleteById(id)
}
