package com.ruhanazevedo.workoutgenerator.domain.model

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroups: List<String>,
    val secondaryMuscleGroups: List<String>,
    val equipment: String,
    val difficulty: Difficulty,
    val instructions: String,
    val youtubeVideoId: String?,
    val isCustom: Boolean,
    val isDeleted: Boolean,
    val createdAt: Long
)
