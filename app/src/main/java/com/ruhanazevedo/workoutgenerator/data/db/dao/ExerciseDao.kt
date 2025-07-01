package com.ruhanazevedo.workoutgenerator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruhanazevedo.workoutgenerator.data.db.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE is_deleted = 0")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("""
        SELECT * FROM exercises
        WHERE is_deleted = 0
          AND (:query = '' OR name LIKE '%' || :query || '%')
          AND (:muscleFilter = '' OR muscle_groups LIKE '%' || :muscleFilter || '%')
        ORDER BY name ASC
    """)
    fun search(query: String, muscleFilter: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id AND is_deleted = 0")
    fun getById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE muscle_groups LIKE '%' || :group || '%' AND is_deleted = 0")
    fun getByMuscleGroup(group: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE LOWER(name) = LOWER(:name) AND is_deleted = 0 LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM workout_plan_exercises WHERE exercise_id = :exerciseId")
    suspend fun countPlanReferences(exerciseId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: ExerciseEntity)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET is_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("""
        SELECT * FROM exercises
        WHERE is_deleted = 0
          AND muscle_groups LIKE '%' || :group || '%'
          AND (:equipmentFilter = '' OR equipment IN (:equipmentList))
          AND difficulty IN (:difficultyList)
    """)
    suspend fun getByMuscleGroupFiltered(
        group: String,
        equipmentFilter: String,
        equipmentList: List<String>,
        difficultyList: List<String>
    ): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises WHERE is_custom = 0 AND is_deleted = 0")
    suspend fun countSeeded(): Int
}
