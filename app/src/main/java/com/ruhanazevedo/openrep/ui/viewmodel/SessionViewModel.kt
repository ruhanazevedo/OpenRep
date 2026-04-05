package com.ruhanazevedo.openrep.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.openrep.data.db.dao.ExerciseDao
import com.ruhanazevedo.openrep.data.db.dao.SessionSetDao
import com.ruhanazevedo.openrep.data.db.dao.WorkoutPlanDao
import com.ruhanazevedo.openrep.data.db.dao.WorkoutSessionDao
import com.ruhanazevedo.openrep.data.db.dao.UserPreferencesDao
import com.ruhanazevedo.openrep.data.db.entity.SessionSetEntity
import com.ruhanazevedo.openrep.data.db.entity.WorkoutPlanExerciseEntity
import com.ruhanazevedo.openrep.data.db.entity.WorkoutSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SessionExerciseItem(
    val planExercise: WorkoutPlanExerciseEntity,
    val exerciseName: String,
    val targetMuscle: String,
    val setsTarget: Int,
    val repsTarget: Int,
    val instructions: String
)

data class SessionUiState(
    val isLoading: Boolean = true,
    val planName: String = "",
    val exercises: List<SessionExerciseItem> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val currentSetNumber: Int = 1,
    val repsInput: String = "",
    val weightInput: String = "",
    val loggedSets: List<SessionSetEntity> = emptyList(),
    val restTimerSeconds: Int = 0,
    val restTimerRunning: Boolean = false,
    val finishCompleted: Boolean = false,
    val sessionId: String = ""
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val workoutPlanDao: WorkoutPlanDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val sessionSetDao: SessionSetDao,
    private val exerciseDao: ExerciseDao,
    private val userPreferencesDao: UserPreferencesDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val planId: String = checkNotNull(savedStateHandle["planId"])

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState

    private var restTimerJob: Job? = null
    private var restDuration: Int = 60

    init {
        viewModelScope.launch {
            val prefs = userPreferencesDao.get().firstOrNull()
            restDuration = prefs?.restTimerSeconds ?: 60

            val planWithExercises = workoutPlanDao.getPlanWithExercises(planId).first()
            if (planWithExercises == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            val plan = planWithExercises.plan
            val sortedExercises = planWithExercises.exercises
                .sortedWith(compareBy({ it.dayIndex }, { it.orderIndex }))

            val items = sortedExercises.map { pe ->
                val entity = exerciseDao.getById(pe.exerciseId).firstOrNull()
                SessionExerciseItem(
                    planExercise = pe,
                    exerciseName = entity?.name ?: "Unknown",
                    targetMuscle = entity?.muscleGroups?.firstOrNull() ?: "",
                    setsTarget = pe.sets,
                    repsTarget = pe.reps,
                    instructions = entity?.instructions ?: ""
                )
            }

            val sessionId = UUID.randomUUID().toString()
            val session = WorkoutSessionEntity(
                id = sessionId,
                planId = planId,
                startedAt = System.currentTimeMillis(),
                completedAt = null,
                notes = ""
            )
            workoutSessionDao.insert(session)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                planName = plan.name,
                exercises = items,
                sessionId = sessionId,
                currentExerciseIndex = 0,
                currentSetNumber = 1
            )
        }
    }

    fun setRepsInput(value: String) {
        _uiState.value = _uiState.value.copy(repsInput = value)
    }

    fun setWeightInput(value: String) {
        _uiState.value = _uiState.value.copy(weightInput = value)
    }

    fun logSet() {
        val state = _uiState.value
        val reps = state.repsInput.trim().toIntOrNull() ?: return
        val weight = state.weightInput.trim().toFloatOrNull()
        val exercise = state.exercises.getOrNull(state.currentExerciseIndex) ?: return

        val setEntity = SessionSetEntity(
            id = UUID.randomUUID().toString(),
            sessionId = state.sessionId,
            planExerciseId = exercise.planExercise.id,
            setNumber = state.currentSetNumber,
            repsCompleted = reps,
            weightKg = weight,
            completedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            sessionSetDao.insert(setEntity)
            val newLogged = state.loggedSets + setEntity

            val nextSetNumber = state.currentSetNumber + 1
            val setsTarget = exercise.setsTarget

            _uiState.value = if (nextSetNumber > setsTarget) {
                // Move to next exercise
                state.copy(
                    loggedSets = newLogged,
                    currentSetNumber = 1,
                    currentExerciseIndex = state.currentExerciseIndex + 1,
                    repsInput = "",
                    weightInput = ""
                )
            } else {
                state.copy(
                    loggedSets = newLogged,
                    currentSetNumber = nextSetNumber,
                    repsInput = "",
                    weightInput = ""
                )
            }

            startRestTimer()
        }
    }

    private fun startRestTimer() {
        restTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(restTimerSeconds = restDuration, restTimerRunning = true)
        restTimerJob = viewModelScope.launch {
            var remaining = restDuration
            while (remaining > 0) {
                delay(1_000)
                remaining--
                _uiState.value = _uiState.value.copy(restTimerSeconds = remaining)
            }
            _uiState.value = _uiState.value.copy(restTimerRunning = false)
        }
    }

    fun skipRest() {
        restTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(restTimerRunning = false, restTimerSeconds = 0)
    }

    fun finishWorkout() {
        val state = _uiState.value
        if (state.isLoading || state.sessionId.isBlank()) return
        restTimerJob?.cancel()
        viewModelScope.launch {
            val existing = workoutSessionDao.getById(state.sessionId).firstOrNull() ?: return@launch
            workoutSessionDao.update(existing.copy(completedAt = System.currentTimeMillis()))
            _uiState.value = _uiState.value.copy(finishCompleted = true)
        }
    }
}
