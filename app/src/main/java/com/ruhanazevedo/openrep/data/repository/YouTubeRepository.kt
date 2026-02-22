package com.ruhanazevedo.openrep.data.repository

import com.ruhanazevedo.openrep.data.remote.YouTubeApiService
import com.ruhanazevedo.openrep.data.remote.YouTubeSearchItem
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
