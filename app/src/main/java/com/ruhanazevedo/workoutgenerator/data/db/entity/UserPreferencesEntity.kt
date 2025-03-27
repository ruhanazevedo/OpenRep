package com.ruhanazevedo.workoutgenerator.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1, // singleton row

    @ColumnInfo(name = "rest_timer_seconds")
    val restTimerSeconds: Int = 60,

    @ColumnInfo(name = "available_equipment")
    val availableEquipment: List<String> = emptyList(), // empty = all

    @ColumnInfo(name = "min_difficulty")
    val minDifficulty: String = "Beginner",

    @ColumnInfo(name = "max_difficulty")
    val maxDifficulty: String = "Advanced",

    @ColumnInfo(name = "exercises_per_muscle_group")
    val exercisesPerMuscleGroup: Int = 3
)
