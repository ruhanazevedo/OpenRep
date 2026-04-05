package com.ruhanazevedo.openrep.data.seeder

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SeedExercise(
    @Json(name = "name") val name: String,
    @Json(name = "muscle_groups") val muscleGroups: List<String>,
    @Json(name = "secondary_muscle_groups") val secondaryMuscleGroups: List<String>,
    @Json(name = "equipment") val equipment: String,
    @Json(name = "difficulty") val difficulty: String,
    @Json(name = "instructions") val instructions: String,
    @Json(name = "youtube_video_id") val youtubeVideoId: String? = null,
    @Json(name = "exercise_type") val exerciseType: String = "STRENGTH",
    @Json(name = "duration_seconds") val durationSeconds: Int? = null
)
