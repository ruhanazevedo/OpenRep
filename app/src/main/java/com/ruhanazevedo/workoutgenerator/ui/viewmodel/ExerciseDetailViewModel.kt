package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.data.remote.RemoteMediaConfigService
import com.ruhanazevedo.workoutgenerator.data.remote.WgerApiService
import com.ruhanazevedo.workoutgenerator.data.repository.ExerciseRepository
import com.ruhanazevedo.workoutgenerator.domain.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val isLoading: Boolean = true,
    val showDeleteConfirm: Boolean = false,
    val deleteCompleted: Boolean = false
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val wgerApiService: WgerApiService,
    private val remoteMediaConfigService: RemoteMediaConfigService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState

    private val _exerciseImages = MutableStateFlow<List<String>>(emptyList())
    val exerciseImages: StateFlow<List<String>> = _exerciseImages

    private val _remoteYoutubeId = MutableStateFlow<String?>(null)
    val remoteYoutubeId: StateFlow<String?> = _remoteYoutubeId

    init {
        viewModelScope.launch {
            repository.getById(exerciseId).collectLatest { exercise ->
                _uiState.value = _uiState.value.copy(exercise = exercise, isLoading = false)
                if (exercise != null) {
                    loadMedia(exercise)
                }
            }
        }
    }

    private fun loadMedia(exercise: Exercise) {
        viewModelScope.launch {
            // Try remote config first (primary source)
            var remoteImages = emptyList<String>()
            try {
                val config = withContext(Dispatchers.IO) {
                    remoteMediaConfigService.getMediaConfig()
                }
                val entry = config.exercises.entries
                    .firstOrNull { it.key.equals(exercise.name, ignoreCase = true) }
                    ?.value
                if (entry != null) {
                    remoteImages = entry.images
                    if (entry.youtubeId != null && exercise.youtubeVideoId == null) {
                        _remoteYoutubeId.value = entry.youtubeId
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaConfig", "Remote config fetch failed", e)
            }

            if (remoteImages.isNotEmpty()) {
                _exerciseImages.value = remoteImages
                return@launch
            }

            // Fallback: Wger API
            try {
                val baseId = withContext(Dispatchers.IO) {
                    wgerApiService.searchExercise(term = exercise.name).suggestions.firstOrNull()?.data?.baseId
                } ?: return@launch
                val images = withContext(Dispatchers.IO) {
                    wgerApiService.getExerciseImages(exerciseBase = baseId).results.map { it.image }
                }
                _exerciseImages.value = images
            } catch (_: Exception) {
                // no images available
            }
        }
    }

    fun requestDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
    }

    fun confirmDelete() {
        viewModelScope.launch {
            repository.softDelete(exerciseId)
            _uiState.value = _uiState.value.copy(showDeleteConfirm = false, deleteCompleted = true)
        }
    }

    fun saveInstructions(instructions: String) {
        val exercise = _uiState.value.exercise ?: return
        viewModelScope.launch {
            repository.update(exercise.copy(instructions = instructions))
        }
    }
}
