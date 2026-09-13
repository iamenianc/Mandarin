package com.learnhuayu.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.learnhuayu.core.data.db.entity.LocalPersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalPersonaDao {
    @Upsert
    suspend fun upsert(persona: LocalPersonaEntity)

    @Query("SELECT * FROM local_personas WHERE id = :id")
    suspend fun byId(id: String): LocalPersonaEntity?

    @Query("SELECT * FROM local_personas ORDER BY label")
    fun all(): Flow<List<LocalPersonaEntity>>

    @Query("DELETE FROM local_personas WHERE id = :id")
    suspend fun deleteById(id: String)
}
