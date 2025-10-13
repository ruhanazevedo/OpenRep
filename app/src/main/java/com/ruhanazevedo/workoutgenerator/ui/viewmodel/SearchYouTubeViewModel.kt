package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.BuildConfig
import com.ruhanazevedo.workoutgenerator.data.remote.YouTubeSearchItem
import com.ruhanazevedo.workoutgenerator.data.repository.ExerciseRepository
import com.ruhanazevedo.workoutgenerator.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchYouTubeUiState(
    val query: String = "",
    val pasteInput: String = "",
    val results: List<YouTubeSearchItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val apiKeyAvailable: Boolean = BuildConfig.YOUTUBE_API_KEY.isNotBlank(),
    val confirmCompleted: Boolean = false
)

@HiltViewModel
class SearchYouTubeViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(SearchYouTubeUiState())
    val uiState: StateFlow<SearchYouTubeUiState> = _uiState

    fun setQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun setPasteInput(value: String) {
        _uiState.value = _uiState.value.copy(pasteInput = value)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        val apiKey = BuildConfig.YOUTUBE_API_KEY
        if (apiKey.isBlank()) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null, results = emptyList())
        viewModelScope.launch {
            youTubeRepository.search(query, apiKey).fold(
                onSuccess = { items ->
                    _uiState.value = _uiState.value.copy(isLoading = false, results = items)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Search failed")
                }
            )
        }
    }

    fun confirmVideoId(videoId: String) {
        viewModelScope.launch {
            val exercise = exerciseRepository.getById(exerciseId).first() ?: return@launch
            if (!exercise.isCustom) return@launch
            exerciseRepository.update(exercise.copy(youtubeVideoId = videoId))
            _uiState.value = _uiState.value.copy(confirmCompleted = true)
        }
    }

    fun confirmPasteInput() {
        val raw = _uiState.value.pasteInput.trim()
        if (raw.isBlank()) return
        val id = parseYouTubeId(raw) ?: return
        confirmVideoId(id)
    }
}

private fun parseYouTubeId(input: String): String? {
    if (input.matches(Regex("[a-zA-Z0-9_-]{11}"))) return input
    val shortUrl = Regex("youtu\\.be/([a-zA-Z0-9_-]{11})").find(input)
    if (shortUrl != null) return shortUrl.groupValues[1]
    val longUrl = Regex("[?&]v=([a-zA-Z0-9_-]{11})").find(input)
    if (longUrl != null) return longUrl.groupValues[1]
    val embed = Regex("embed/([a-zA-Z0-9_-]{11})").find(input)
    if (embed != null) return embed.groupValues[1]
    return null
}
