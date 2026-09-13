package com.learnhuayu.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.learnhuayu.core.data.db.entity.MissionSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionSessionDao {
    @Upsert
    suspend fun upsert(session: MissionSessionEntity)

    @Query("SELECT * FROM mission_sessions WHERE id = :id")
    suspend fun byId(id: String): MissionSessionEntity?

    @Query("SELECT * FROM mission_sessions WHERE missionId = :missionId ORDER BY startedAt")
    fun byMission(missionId: String): Flow<List<MissionSessionEntity>>

    @Query("DELETE FROM mission_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}
