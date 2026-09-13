package com.learnhuayu.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.learnhuayu.core.data.db.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("SELECT * FROM progress WHERE contentItemId = :contentItemId")
    suspend fun byId(contentItemId: String): ProgressEntity?

    @Query("SELECT * FROM progress ORDER BY contentItemId")
    fun all(): Flow<List<ProgressEntity>>

    @Query("DELETE FROM progress WHERE contentItemId = :contentItemId")
    suspend fun deleteById(contentItemId: String)
}
