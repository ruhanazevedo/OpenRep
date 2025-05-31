package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.data.repository.ExerciseRepository
import com.ruhanazevedo.workoutgenerator.domain.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.getById(exerciseId).collectLatest { exercise ->
                _uiState.value = _uiState.value.copy(exercise = exercise, isLoading = false)
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
