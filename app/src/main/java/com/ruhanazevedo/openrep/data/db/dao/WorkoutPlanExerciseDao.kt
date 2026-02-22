package com.ruhanazevedo.openrep.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruhanazevedo.openrep.data.db.entity.WorkoutPlanExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanExerciseDao {
    @Query("SELECT * FROM workout_plan_exercises WHERE plan_id = :planId ORDER BY day_index, order_index")
    fun getByPlanId(planId: String): Flow<List<WorkoutPlanExerciseEntity>>

    @Query("SELECT * FROM workout_plan_exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkoutPlanExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<WorkoutPlanExerciseEntity>)

    @Query("DELETE FROM workout_plan_exercises WHERE plan_id = :planId")
    suspend fun deleteByPlanId(planId: String)
}
