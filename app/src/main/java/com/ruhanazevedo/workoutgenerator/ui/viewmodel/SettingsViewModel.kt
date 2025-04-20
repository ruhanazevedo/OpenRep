package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.data.db.entity.UserPreferencesEntity
import com.ruhanazevedo.workoutgenerator.data.repository.PreferencesRepository
import com.ruhanazevedo.workoutgenerator.domain.model.Difficulty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    fun setMinDifficulty(difficulty: String) = update { current ->
        val newMin = Difficulty.from(difficulty)
        val currentMax = Difficulty.from(current.maxDifficulty)
        if (newMin <= currentMax) current.copy(minDifficulty = difficulty) else current
    }

    fun setMaxDifficulty(difficulty: String) = update { current ->
        val newMax = Difficulty.from(difficulty)
        val currentMin = Difficulty.from(current.minDifficulty)
        if (newMax >= currentMin) current.copy(maxDifficulty = difficulty) else current
    }

    fun setExercisesPerMuscleGroup(count: Int) = update { it.copy(exercisesPerMuscleGroup = count) }

    private fun update(transform: (UserPreferencesEntity) -> UserPreferencesEntity) {
        viewModelScope.launch {
            val current = repository.preferences.first()
            repository.update(transform(current))
        }
    }

    init {
        viewModelScope.launch { repository.ensureDefaults() }
    }
}
