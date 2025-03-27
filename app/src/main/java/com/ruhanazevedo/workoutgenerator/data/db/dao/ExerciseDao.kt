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

    @Query("SELECT * FROM exercises WHERE id = :id AND is_deleted = 0")
    fun getById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE muscle_groups LIKE '%' || :group || '%' AND is_deleted = 0")
    fun getByMuscleGroup(group: String): Flow<List<ExerciseEntity>>

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

    @Query("SELECT COUNT(*) FROM exercises WHERE is_custom = 0 AND is_deleted = 0")
    suspend fun countSeeded(): Int
}
