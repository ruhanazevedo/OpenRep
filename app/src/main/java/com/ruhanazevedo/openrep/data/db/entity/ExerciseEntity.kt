package com.ruhanazevedo.openrep.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "muscle_groups")
    val muscleGroups: List<String>,

    @ColumnInfo(name = "secondary_muscle_groups")
    val secondaryMuscleGroups: List<String>,

    @ColumnInfo(name = "equipment")
    val equipment: String,

    @ColumnInfo(name = "difficulty")
    val difficulty: String,

    @ColumnInfo(name = "instructions")
    val instructions: String,

    @ColumnInfo(name = "youtube_video_id")
    val youtubeVideoId: String?,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
