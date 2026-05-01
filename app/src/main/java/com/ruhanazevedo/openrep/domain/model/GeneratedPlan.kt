package com.ruhanazevedo.openrep.domain.model

data class GeneratedExercise(
    val exerciseId: String,
    val name: String,
    val targetMuscle: String,
    val equipment: String,
    val sets: Int,
    val reps: Int,
    val youtubeVideoId: String?
)

data class WarmupCooldownItem(
    val name: String,
    val durationSeconds: Int,
    val description: String
)

data class GeneratedDay(
    val dayIndex: Int,
    val label: String,
    val exercises: List<GeneratedExercise>,
    val warmup: List<WarmupCooldownItem> = emptyList(),
    val cooldown: List<WarmupCooldownItem> = emptyList(),
    val estimatedMinutes: Int = 0
)

data class GeneratedPlan(
    val splitType: SplitType,
    val daysPerWeek: Int,
    val muscleGroups: List<String>,
    val days: List<GeneratedDay>
)
