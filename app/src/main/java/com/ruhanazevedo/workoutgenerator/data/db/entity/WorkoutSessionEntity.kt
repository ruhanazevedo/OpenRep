package com.ruhanazevedo.workoutgenerator.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["plan_id"])]
)
data class WorkoutSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "plan_id")
    val planId: String?,

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,

    @ColumnInfo(name = "notes")
    val notes: String
)
