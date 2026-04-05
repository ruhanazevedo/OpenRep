package com.ruhanazevedo.openrep.data.mapper

import com.ruhanazevedo.openrep.data.db.entity.ExerciseEntity
import com.ruhanazevedo.openrep.domain.model.Difficulty
import com.ruhanazevedo.openrep.domain.model.Exercise
import com.ruhanazevedo.openrep.domain.model.ExerciseType

fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    muscleGroups = muscleGroups,
    secondaryMuscleGroups = secondaryMuscleGroups,
    equipment = equipment,
    difficulty = Difficulty.from(difficulty),
    instructions = instructions,
    youtubeVideoId = youtubeVideoId,
    isCustom = isCustom,
    isDeleted = isDeleted,
    createdAt = createdAt,
    exerciseType = ExerciseType.from(exerciseType),
    durationSeconds = durationSeconds
)

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    muscleGroups = muscleGroups,
    secondaryMuscleGroups = secondaryMuscleGroups,
    equipment = equipment,
    difficulty = difficulty.name,
    instructions = instructions,
    youtubeVideoId = youtubeVideoId,
    isCustom = isCustom,
    isDeleted = isDeleted,
    createdAt = createdAt,
    exerciseType = exerciseType.name,
    durationSeconds = durationSeconds
)
