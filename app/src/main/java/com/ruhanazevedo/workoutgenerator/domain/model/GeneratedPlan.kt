package com.ruhanazevedo.workoutgenerator.domain.model

data class GeneratedExercise(
    val exerciseId: String,
    val name: String,
    val targetMuscle: String,
    val equipment: String,
    val sets: Int,
    val reps: Int,
    val youtubeVideoId: String?
)

data class GeneratedDay(
    val dayIndex: Int,
    val label: String,
    val exercises: List<GeneratedExercise>
)

data class GeneratedPlan(
    val splitType: SplitType,
    val daysPerWeek: Int,
    val muscleGroups: List<String>,
    val days: List<GeneratedDay>
)
