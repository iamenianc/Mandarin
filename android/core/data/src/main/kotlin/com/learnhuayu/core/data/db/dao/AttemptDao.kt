package com.learnhuayu.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.learnhuayu.core.data.db.entity.AttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Upsert
    suspend fun upsert(attempt: AttemptEntity)

    @Query("SELECT * FROM attempts WHERE id = :id")
    suspend fun byId(id: String): AttemptEntity?

    @Query("SELECT * FROM attempts WHERE contentItemId = :contentItemId ORDER BY recordedAt DESC")
    fun byContentItem(contentItemId: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts ORDER BY recordedAt DESC")
    fun all(): Flow<List<AttemptEntity>>

    @Query("DELETE FROM attempts WHERE id = :id")
    suspend fun deleteById(id: String)
}
