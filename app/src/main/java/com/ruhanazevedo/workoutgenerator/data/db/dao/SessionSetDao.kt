package com.ruhanazevedo.workoutgenerator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruhanazevedo.workoutgenerator.data.db.entity.SessionSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionSetDao {
    @Query("SELECT * FROM session_sets WHERE session_id = :sessionId ORDER BY set_number")
    fun getBySessionId(sessionId: String): Flow<List<SessionSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: SessionSetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<SessionSetEntity>)

    @Query("DELETE FROM session_sets WHERE session_id = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
}
