package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.data.repository.ExerciseRepository
import com.ruhanazevedo.workoutgenerator.domain.model.Difficulty
import com.ruhanazevedo.workoutgenerator.domain.model.Equipment
import com.ruhanazevedo.workoutgenerator.domain.model.Exercise
import com.ruhanazevedo.workoutgenerator.domain.model.MuscleGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddExerciseUiState(
    val name: String = "",
    val muscleGroups: Set<String> = emptySet(),
    val secondaryMuscleGroups: Set<String> = emptySet(),
    val equipment: String = Equipment.BARBELL,
    val difficulty: String = Difficulty.Beginner.name,
    val instructions: String = "",
    val youtubeInput: String = "",
    val nameError: String? = null,
    val muscleGroupError: String? = null,
    val isEditMode: Boolean = false,
    val saveCompleted: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class AddExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: String? = savedStateHandle["exerciseId"]

    private val _uiState = MutableStateFlow(AddExerciseUiState())
    val uiState: StateFlow<AddExerciseUiState> = _uiState

    init {
        if (exerciseId != null) {
            _uiState.value = _uiState.value.copy(isLoading = true, isEditMode = true)
            viewModelScope.launch {
                val exercise = repository.getById(exerciseId).first()
                if (exercise != null) {
                    _uiState.value = _uiState.value.copy(
                        name = exercise.name,
                        muscleGroups = exercise.muscleGroups.toSet(),
                        secondaryMuscleGroups = exercise.secondaryMuscleGroups.toSet(),
                        equipment = exercise.equipment,
                        difficulty = exercise.difficulty.name,
                        instructions = exercise.instructions,
                        youtubeInput = exercise.youtubeVideoId ?: "",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun setName(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null)
    }

    fun toggleMuscleGroup(muscle: String) {
        val current = _uiState.value.muscleGroups.toMutableSet()
        if (muscle in current) current.remove(muscle) else current.add(muscle)
        _uiState.value = _uiState.value.copy(muscleGroups = current, muscleGroupError = null)
    }

    fun toggleSecondaryMuscleGroup(muscle: String) {
        val current = _uiState.value.secondaryMuscleGroups.toMutableSet()
        if (muscle in current) current.remove(muscle) else current.add(muscle)
        _uiState.value = _uiState.value.copy(secondaryMuscleGroups = current)
    }

    fun setEquipment(value: String) {
        _uiState.value = _uiState.value.copy(equipment = value)
    }

    fun setDifficulty(value: String) {
        _uiState.value = _uiState.value.copy(difficulty = value)
    }

    fun setInstructions(value: String) {
        _uiState.value = _uiState.value.copy(instructions = value)
    }

    fun setYoutubeInput(value: String) {
        _uiState.value = _uiState.value.copy(youtubeInput = value)
    }

    fun save() {
        val state = _uiState.value
        var hasError = false

        if (state.name.isBlank()) {
            _uiState.value = _uiState.value.copy(nameError = "Name is required")
            hasError = true
        }

        if (state.muscleGroups.isEmpty()) {
            _uiState.value = _uiState.value.copy(muscleGroupError = "At least one muscle group is required")
            hasError = true
        } else {
            val invalid = state.muscleGroups.filter { it !in MuscleGroup.ALL }
            if (invalid.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(muscleGroupError = "Invalid: ${invalid.joinToString()}")
                hasError = true
            }
        }

        if (hasError) return

        val videoId = state.youtubeInput.trim().ifBlank { null }?.let { extractYouTubeId(it) }

        viewModelScope.launch {
            if (exerciseId != null) {
                val existing = repository.getById(exerciseId).first() ?: return@launch
                repository.update(
                    existing.copy(
                        name = state.name.trim(),
                        muscleGroups = state.muscleGroups.toList(),
                        secondaryMuscleGroups = state.secondaryMuscleGroups.toList(),
                        equipment = state.equipment,
                        difficulty = Difficulty.from(state.difficulty),
                        instructions = state.instructions.trim(),
                        youtubeVideoId = videoId
                    )
                )
            } else {
                repository.insert(
                    Exercise(
                        id = UUID.randomUUID().toString(),
                        name = state.name.trim(),
                        muscleGroups = state.muscleGroups.toList(),
                        secondaryMuscleGroups = state.secondaryMuscleGroups.toList(),
                        equipment = state.equipment,
                        difficulty = Difficulty.from(state.difficulty),
                        instructions = state.instructions.trim(),
                        youtubeVideoId = videoId,
                        isCustom = true,
                        isDeleted = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            _uiState.value = _uiState.value.copy(saveCompleted = true)
        }
    }
}
