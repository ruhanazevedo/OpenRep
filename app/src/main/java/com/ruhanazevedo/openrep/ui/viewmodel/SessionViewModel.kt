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
    val exerciseId: String,
    val exerciseName: String,
    val targetMuscle: String,
    val setsTarget: Int,
    val repsTarget: Int,
    val instructions: String
)

data class SessionDay(
    val dayIndex: Int,
    val label: String,
    val muscleGroups: String,
    val exercises: List<SessionExerciseItem>
)

data class ExerciseSessionState(
    val isExpanded: Boolean = false,
    val currentSetNumber: Int = 1,
    val repsInput: String = "",
    val weightInput: String = "",
    val loggedSets: List<SessionSetEntity> = emptyList()
)

data class SessionUiState(
    val isLoading: Boolean = true,
    val planName: String = "",
    val days: List<SessionDay> = emptyList(),
    val selectedDayIndex: Int? = null,
    val exerciseStates: Map<String, ExerciseSessionState> = emptyMap(),
    val restTimerSeconds: Int = 0,
    val restTimerRunning: Boolean = false,
    val finishCompleted: Boolean = false,
    val sessionId: String = "",
    val elapsedSeconds: Int = 0
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
    private var elapsedTimerJob: Job? = null
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
                    exerciseId = pe.exerciseId,
                    exerciseName = entity?.name ?: "Unknown",
                    targetMuscle = entity?.muscleGroups?.firstOrNull() ?: "",
                    setsTarget = pe.sets,
                    repsTarget = pe.reps,
                    instructions = entity?.instructions ?: ""
                )
            }

            val days = items
                .groupBy { it.planExercise.dayIndex }
                .entries
                .sortedBy { it.key }
                .map { (dayIndex, dayExercises) ->
                    val muscles = dayExercises
                        .map { it.targetMuscle }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(3)
                        .joinToString(" • ")
                    SessionDay(
                        dayIndex = dayIndex,
                        label = "Day ${dayIndex + 1}",
                        muscleGroups = muscles,
                        exercises = dayExercises
                    )
                }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                planName = plan.name,
                days = days,
                selectedDayIndex = null
            )
        }
    }

    fun selectDay(dayIndex: Int) {
        viewModelScope.launch {
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
                selectedDayIndex = dayIndex,
                sessionId = sessionId,
                elapsedSeconds = 0
            )

            startElapsedTimer()
        }
    }

    private fun startElapsedTimer() {
        elapsedTimerJob?.cancel()
        elapsedTimerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _uiState.value = _uiState.value.copy(elapsedSeconds = _uiState.value.elapsedSeconds + 1)
            }
        }
    }

    fun toggleExpand(planExerciseId: String) {
        val current = _uiState.value.exerciseStates[planExerciseId] ?: ExerciseSessionState()
        val updated = _uiState.value.exerciseStates + (planExerciseId to current.copy(isExpanded = !current.isExpanded))
        _uiState.value = _uiState.value.copy(exerciseStates = updated)
    }

    fun setRepsInput(planExerciseId: String, value: String) {
        val current = _uiState.value.exerciseStates[planExerciseId] ?: ExerciseSessionState()
        val updated = _uiState.value.exerciseStates + (planExerciseId to current.copy(repsInput = value))
        _uiState.value = _uiState.value.copy(exerciseStates = updated)
    }

    fun setWeightInput(planExerciseId: String, value: String) {
        val current = _uiState.value.exerciseStates[planExerciseId] ?: ExerciseSessionState()
        val updated = _uiState.value.exerciseStates + (planExerciseId to current.copy(weightInput = value))
        _uiState.value = _uiState.value.copy(exerciseStates = updated)
    }

    fun logSet(planExerciseId: String) {
        val state = _uiState.value
        val exState = state.exerciseStates[planExerciseId] ?: ExerciseSessionState()
        val reps = exState.repsInput.trim().toIntOrNull() ?: return
        val weight = exState.weightInput.trim().toFloatOrNull()

        val day = state.days.find { it.dayIndex == state.selectedDayIndex } ?: return
        val exercise = day.exercises.find { it.planExercise.id == planExerciseId } ?: return

        val setEntity = SessionSetEntity(
            id = UUID.randomUUID().toString(),
            sessionId = state.sessionId,
            planExerciseId = planExerciseId,
            setNumber = exState.currentSetNumber,
            repsCompleted = reps,
            weightKg = weight,
            completedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            sessionSetDao.insert(setEntity)
            val newLogged = exState.loggedSets + setEntity
            val nextSetNumber = exState.currentSetNumber + 1
            val allSetsDone = newLogged.size >= exercise.setsTarget

            val newExState = exState.copy(
                loggedSets = newLogged,
                currentSetNumber = if (allSetsDone) exState.currentSetNumber else nextSetNumber,
                repsInput = "",
                weightInput = "",
                isExpanded = !allSetsDone
            )

            val updatedStates = state.exerciseStates + (planExerciseId to newExState)
            _uiState.value = _uiState.value.copy(exerciseStates = updatedStates)

            startRestTimer()
        }
    }

    fun quickComplete(planExerciseId: String) {
        val state = _uiState.value
        val day = state.days.find { it.dayIndex == state.selectedDayIndex } ?: return
        val exercise = day.exercises.find { it.planExercise.id == planExerciseId } ?: return
        val exState = state.exerciseStates[planExerciseId] ?: ExerciseSessionState()

        val alreadyLogged = exState.loggedSets.size
        if (alreadyLogged >= exercise.setsTarget) return

        viewModelScope.launch {
            val newSets = mutableListOf<SessionSetEntity>()
            for (i in alreadyLogged until exercise.setsTarget) {
                val setEntity = SessionSetEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = state.sessionId,
                    planExerciseId = planExerciseId,
                    setNumber = i + 1,
                    repsCompleted = exercise.repsTarget,
                    weightKg = null,
                    completedAt = System.currentTimeMillis()
                )
                sessionSetDao.insert(setEntity)
                newSets.add(setEntity)
            }

            val newExState = exState.copy(
                loggedSets = exState.loggedSets + newSets,
                isExpanded = false,
                repsInput = "",
                weightInput = ""
            )
            val updatedStates = _uiState.value.exerciseStates + (planExerciseId to newExState)
            _uiState.value = _uiState.value.copy(exerciseStates = updatedStates)
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
        elapsedTimerJob?.cancel()
        viewModelScope.launch {
            val existing = workoutSessionDao.getById(state.sessionId).firstOrNull() ?: return@launch
            workoutSessionDao.update(existing.copy(completedAt = System.currentTimeMillis()))
            _uiState.value = _uiState.value.copy(finishCompleted = true)
        }
    }
}
