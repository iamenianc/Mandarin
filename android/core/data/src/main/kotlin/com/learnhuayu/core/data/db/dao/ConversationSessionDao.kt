package com.learnhuayu.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.learnhuayu.core.data.db.entity.ConversationSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationSessionDao {
    @Upsert
    suspend fun upsert(session: ConversationSessionEntity)

    @Query("SELECT * FROM conversation_sessions WHERE id = :id")
    suspend fun byId(id: String): ConversationSessionEntity?

    @Query("SELECT * FROM conversation_sessions ORDER BY startedAt DESC")
    fun all(): Flow<List<ConversationSessionEntity>>

    @Query("DELETE FROM conversation_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}
