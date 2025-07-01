package com.ruhanazevedo.workoutgenerator.domain.model

data class GenerationInput(
    val daysPerWeek: Int,
    val muscleGroups: List<String>,
    val splitType: SplitType,
    val exercisesPerMuscle: Int = 3
)
