package com.ruhanazevedo.openrep.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutPlanWithExercises(
    @Embedded val plan: WorkoutPlanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "plan_id"
    )
    val exercises: List<WorkoutPlanExerciseEntity>
)
