package com.learnhuayu.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.learnhuayu.core.data.db.entity.MissionTurnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionTurnDao {
    @Upsert
    suspend fun upsert(turn: MissionTurnEntity)

    @Query("SELECT * FROM mission_turns WHERE id = :id")
    suspend fun byId(id: String): MissionTurnEntity?

    @Query("SELECT * FROM mission_turns WHERE sessionId = :sessionId ORDER BY rowid")
    fun bySession(sessionId: String): Flow<List<MissionTurnEntity>>

    @Query("DELETE FROM mission_turns WHERE id = :id")
    suspend fun deleteById(id: String)
}
