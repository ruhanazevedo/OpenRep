package com.ruhanazevedo.workoutgenerator.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class ExerciseMediaConfig(val exercises: Map<String, ExerciseMediaEntry>)

@JsonClass(generateAdapter = true)
data class ExerciseMediaEntry(val images: List<String>, val youtubeId: String?)

interface RemoteMediaConfigService {
    @GET("ruhanazevedo/workout-generator/main/exercise-media-config.json")
    suspend fun getMediaConfig(): ExerciseMediaConfig
}
