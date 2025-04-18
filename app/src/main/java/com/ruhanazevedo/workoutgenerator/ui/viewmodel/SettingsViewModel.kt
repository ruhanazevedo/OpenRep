package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.data.db.entity.UserPreferencesEntity
import com.ruhanazevedo.workoutgenerator.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferencesEntity> = repository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferencesEntity()
        )

    fun setRestTimerSeconds(seconds: Int) = update { it.copy(restTimerSeconds = seconds) }

    fun setAvailableEquipment(equipment: List<String>) = update { it.copy(availableEquipment = equipment) }

    fun setMinDifficulty(difficulty: String) = update { it.copy(minDifficulty = difficulty) }

    fun setMaxDifficulty(difficulty: String) = update { it.copy(maxDifficulty = difficulty) }

    fun setExercisesPerMuscleGroup(count: Int) = update { it.copy(exercisesPerMuscleGroup = count) }

    private fun update(transform: (UserPreferencesEntity) -> UserPreferencesEntity) {
        viewModelScope.launch {
            repository.update(transform(preferences.value))
        }
    }

    init {
        viewModelScope.launch { repository.ensureDefaults() }
    }
}
