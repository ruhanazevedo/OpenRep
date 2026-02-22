package com.ruhanazevedo.openrep.data.repository

import com.ruhanazevedo.openrep.data.db.dao.ExerciseDao
import com.ruhanazevedo.openrep.data.mapper.toDomain
import com.ruhanazevedo.openrep.data.mapper.toEntity
import com.ruhanazevedo.openrep.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao
) {
    fun getAll(): Flow<List<Exercise>> = exerciseDao.getAll().map { list ->
        list.map { it.toDomain() }
    }

    fun search(query: String, muscleFilter: String): Flow<List<Exercise>> =
        exerciseDao.search(query, muscleFilter).map { list -> list.map { it.toDomain() } }

    fun getById(id: String): Flow<Exercise?> = exerciseDao.getById(id).map { it?.toDomain() }

    fun getByMuscleGroup(group: String): Flow<List<Exercise>> =
        exerciseDao.getByMuscleGroup(group).map { list -> list.map { it.toDomain() } }

    suspend fun findByNameIgnoreCase(name: String): Exercise? =
        exerciseDao.findByNameIgnoreCase(name)?.toDomain()

    suspend fun countPlanReferences(exerciseId: String): Int =
        exerciseDao.countPlanReferences(exerciseId)

    suspend fun insert(exercise: Exercise) = exerciseDao.insert(exercise.toEntity())

    suspend fun update(exercise: Exercise) = exerciseDao.update(exercise.toEntity())

    suspend fun softDelete(id: String) = exerciseDao.softDelete(id)

    suspend fun upsert(exercise: Exercise) = exerciseDao.upsert(exercise.toEntity())

    suspend fun insertAll(exercises: List<Exercise>) =
        exerciseDao.insertAll(exercises.map { it.toEntity() })

    suspend fun countSeeded(): Int = exerciseDao.countSeeded()
}
