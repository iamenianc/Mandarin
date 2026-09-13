package com.learnhuayu.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.learnhuayu.core.data.db.entity.DebriefEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebriefEntryDao {
    @Upsert
    suspend fun upsert(entry: DebriefEntryEntity)

    @Query("SELECT * FROM debrief_entries WHERE id = :id")
    suspend fun byId(id: String): DebriefEntryEntity?

    @Query("SELECT * FROM debrief_entries WHERE missionId = :missionId ORDER BY createdAt")
    fun byMission(missionId: String): Flow<List<DebriefEntryEntity>>

    @Query("SELECT * FROM debrief_entries ORDER BY createdAt DESC")
    fun all(): Flow<List<DebriefEntryEntity>>

    @Query("DELETE FROM debrief_entries WHERE id = :id")
    suspend fun deleteById(id: String)
}
