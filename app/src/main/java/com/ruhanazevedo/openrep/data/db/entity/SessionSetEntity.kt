package com.ruhanazevedo.openrep.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutPlanExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_exercise_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["plan_exercise_id"])
    ]
)
data class SessionSetEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "plan_exercise_id")
    val planExerciseId: String?,

    @ColumnInfo(name = "set_number")
    val setNumber: Int,

    @ColumnInfo(name = "reps_completed")
    val repsCompleted: Int,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Float?,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long
)
