package com.ruhanazevedo.workoutgenerator.data.mapper

import com.ruhanazevedo.workoutgenerator.data.db.entity.ExerciseEntity
import com.ruhanazevedo.workoutgenerator.domain.model.Difficulty
import com.ruhanazevedo.workoutgenerator.domain.model.Exercise

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
    createdAt = createdAt
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
    createdAt = createdAt
)
