package com.ruhanazevedo.openrep.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruhanazevedo.openrep.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY started_at DESC")
    fun getAll(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun getById(id: String): Flow<WorkoutSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSessionEntity)

    @Update
    suspend fun update(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM workout_sessions WHERE plan_id = :planId AND day_index = :dayIndex AND completed_at IS NOT NULL ORDER BY completed_at DESC LIMIT 1")
    suspend fun getLastCompletedForDay(planId: String, dayIndex: Int): WorkoutSessionEntity?
}
