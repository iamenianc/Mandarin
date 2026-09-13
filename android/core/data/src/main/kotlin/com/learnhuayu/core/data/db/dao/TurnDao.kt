package com.learnhuayu.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.learnhuayu.core.data.db.entity.TurnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TurnDao {
    @Upsert
    suspend fun upsert(turn: TurnEntity)

    @Query("SELECT * FROM turns WHERE id = :id")
    suspend fun byId(id: String): TurnEntity?

    @Query("SELECT * FROM turns WHERE sessionId = :sessionId ORDER BY rowid")
    fun bySession(sessionId: String): Flow<List<TurnEntity>>

    @Query("DELETE FROM turns WHERE id = :id")
    suspend fun deleteById(id: String)
}
