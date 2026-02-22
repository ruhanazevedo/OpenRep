package com.ruhanazevedo.openrep.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    @Json(name = "items") val items: List<YouTubeSearchItem>
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchItem(
    @Json(name = "id") val id: YouTubeVideoId,
    @Json(name = "snippet") val snippet: YouTubeSnippet
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoId(
    @Json(name = "videoId") val videoId: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
    @Json(name = "title") val title: String,
    @Json(name = "channelTitle") val channelTitle: String,
    @Json(name = "thumbnails") val thumbnails: YouTubeThumbnails
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    @Json(name = "default") val default: YouTubeThumbnail?
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnail(
    @Json(name = "url") val url: String
)

interface YouTubeApiService {
    @GET("search")
    suspend fun search(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 10,
        @Query("q") query: String,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse
}
