package com.ruhanazevedo.openrep.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class WgerSearchResponse(
    @Json(name = "suggestions") val suggestions: List<WgerSuggestion>
)

@JsonClass(generateAdapter = true)
data class WgerSuggestion(
    @Json(name = "data") val data: WgerExerciseData
)

@JsonClass(generateAdapter = true)
data class WgerExerciseData(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "base_id") val baseId: Int
)

@JsonClass(generateAdapter = true)
data class WgerImageResponse(
    @Json(name = "results") val results: List<WgerImage>
)

@JsonClass(generateAdapter = true)
data class WgerImage(
    @Json(name = "image") val image: String
)

interface WgerApiService {
    @GET("api/v2/exercise/search/")
    suspend fun searchExercise(
        @Query("term") term: String,
        @Query("language") language: String = "english",
        @Query("format") format: String = "json"
    ): WgerSearchResponse

    @GET("api/v2/exerciseimage/")
    suspend fun getExerciseImages(
        @Query("exercise_base") exerciseBase: Int,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5
    ): WgerImageResponse
}
