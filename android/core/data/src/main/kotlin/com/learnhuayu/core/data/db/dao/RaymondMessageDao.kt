package com.learnhuayu.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.learnhuayu.core.data.db.entity.RaymondMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RaymondMessageDao {
    @Upsert
    suspend fun upsert(message: RaymondMessageEntity)

    @Query("SELECT * FROM raymond_messages WHERE id = :id")
    suspend fun byId(id: String): RaymondMessageEntity?

    @Query("SELECT * FROM raymond_messages ORDER BY createdAt")
    fun all(): Flow<List<RaymondMessageEntity>>

    @Query("DELETE FROM raymond_messages WHERE id = :id")
    suspend fun deleteById(id: String)
}
