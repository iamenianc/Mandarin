package com.learnhuayu.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.learnhuayu.core.data.db.entity.FieldMissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldMissionDao {
    @Upsert
    suspend fun upsert(mission: FieldMissionEntity)

    @Query("SELECT * FROM field_missions WHERE id = :id")
    suspend fun byId(id: String): FieldMissionEntity?

    @Query("SELECT * FROM field_missions ORDER BY date DESC")
    fun all(): Flow<List<FieldMissionEntity>>

    @Query("DELETE FROM field_missions WHERE id = :id")
    suspend fun deleteById(id: String)
}
