package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.data.repository.PreferencesRepository
import com.ruhanazevedo.workoutgenerator.domain.model.GenerationInput
import com.ruhanazevedo.workoutgenerator.domain.model.MuscleGroup
import com.ruhanazevedo.workoutgenerator.domain.model.SplitType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenerateFilterUiState(
    val daysPerWeek: Int = 3,
    val selectedMuscleGroups: Set<String> = emptySet(),
    val splitType: SplitType = SplitType.A,
    val exercisesPerMuscle: Int = 3,
    val isLoaded: Boolean = false
)

@HiltViewModel
class GenerateFilterViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateFilterUiState())
    val uiState: StateFlow<GenerateFilterUiState> = _uiState

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            _uiState.value = _uiState.value.copy(
                daysPerWeek = prefs.lastDaysPerWeek,
                selectedMuscleGroups = prefs.lastSelectedMuscles.toSet(),
                splitType = SplitType.entries.firstOrNull { it.name == prefs.lastSplitType } ?: SplitType.A,
                exercisesPerMuscle = prefs.exercisesPerMuscleGroup,
                isLoaded = true
            )
        }
    }

    fun setDaysPerWeek(days: Int) {
        _uiState.value = _uiState.value.copy(daysPerWeek = days.coerceIn(1, 7))
    }

    fun toggleMuscleGroup(muscle: String) {
        val current = _uiState.value.selectedMuscleGroups.toMutableSet()
        if (muscle in current) current.remove(muscle) else current.add(muscle)
        _uiState.value = _uiState.value.copy(selectedMuscleGroups = current)
    }

    fun selectAllMuscleGroups() {
        _uiState.value = _uiState.value.copy(
            selectedMuscleGroups = MuscleGroup.ALL.toSet()
        )
    }

    fun clearMuscleGroups() {
        _uiState.value = _uiState.value.copy(selectedMuscleGroups = emptySet())
    }

    fun setSplitType(split: SplitType) {
        _uiState.value = _uiState.value.copy(splitType = split)
    }

    fun buildGenerationInput(): GenerationInput {
        val state = _uiState.value
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            preferencesRepository.update(
                prefs.copy(
                    lastDaysPerWeek = state.daysPerWeek,
                    lastSplitType = state.splitType.name,
                    lastSelectedMuscles = state.selectedMuscleGroups.toList()
                )
            )
        }
        return GenerationInput(
            daysPerWeek = state.daysPerWeek,
            muscleGroups = state.selectedMuscleGroups.toList(),
            splitType = state.splitType,
            exercisesPerMuscle = state.exercisesPerMuscle
        )
    }
}
