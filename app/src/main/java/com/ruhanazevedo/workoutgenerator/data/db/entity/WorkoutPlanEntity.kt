package com.ruhanazevedo.workoutgenerator.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "split_type")
    val splitType: String,

    @ColumnInfo(name = "days_per_week")
    val daysPerWeek: Int,

    @ColumnInfo(name = "muscle_groups")
    val muscleGroups: List<String>,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "is_template")
    val isTemplate: Boolean
)
