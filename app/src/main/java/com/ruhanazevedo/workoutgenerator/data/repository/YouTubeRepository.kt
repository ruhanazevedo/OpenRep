package com.ruhanazevedo.workoutgenerator.data.repository

import com.ruhanazevedo.workoutgenerator.data.remote.YouTubeApiService
import com.ruhanazevedo.workoutgenerator.data.remote.YouTubeSearchItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepository @Inject constructor(
    private val api: YouTubeApiService
) {
    suspend fun search(query: String, apiKey: String): Result<List<YouTubeSearchItem>> {
        return runCatching {
            api.search(query = query, apiKey = apiKey).items
                .filter { it.id.videoId != null }
        }
    }
}
