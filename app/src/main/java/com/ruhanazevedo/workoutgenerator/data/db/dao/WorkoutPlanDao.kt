package com.ruhanazevedo.workoutgenerator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutPlanEntity
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutPlanWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {
    @Query("SELECT * FROM workout_plans ORDER BY created_at DESC")
    fun getAll(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans WHERE id = :id")
    fun getById(id: String): Flow<WorkoutPlanEntity?>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id = :planId")
    fun getPlanWithExercises(planId: String): Flow<WorkoutPlanWithExercises?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WorkoutPlanEntity)

    @Update
    suspend fun update(plan: WorkoutPlanEntity)

    @Query("DELETE FROM workout_plans WHERE id = :id")
    suspend fun delete(id: String)
}
