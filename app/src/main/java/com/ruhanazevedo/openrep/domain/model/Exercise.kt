package com.ruhanazevedo.openrep.domain.model

enum class ExerciseType {
    STRENGTH, WARM_UP, STRETCH;

    companion object {
        fun from(value: String): ExerciseType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: STRENGTH
    }
}

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
    val createdAt: Long,
    val exerciseType: ExerciseType = ExerciseType.STRENGTH,
    val durationSeconds: Int? = null
)
