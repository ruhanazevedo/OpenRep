package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.data.db.dao.ExerciseDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutPlanDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutPlanExerciseDao
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutPlanEntity
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutPlanExerciseEntity
import com.ruhanazevedo.workoutgenerator.data.repository.PreferencesRepository
import com.ruhanazevedo.workoutgenerator.domain.engine.WorkoutGeneratorEngine
import com.ruhanazevedo.workoutgenerator.domain.model.Difficulty
import com.ruhanazevedo.workoutgenerator.domain.model.GeneratedDay
import com.ruhanazevedo.workoutgenerator.domain.model.GeneratedExercise
import com.ruhanazevedo.workoutgenerator.domain.model.GeneratedPlan
import com.ruhanazevedo.workoutgenerator.domain.model.GenerationInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class GeneratedPlanUiState {
    data object Loading : GeneratedPlanUiState()
    data class Success(val plan: GeneratedPlan) : GeneratedPlanUiState()
    data class Error(val message: String) : GeneratedPlanUiState()
    data object Saved : GeneratedPlanUiState()
}

data class SwapSheetState(
    val dayIndex: Int = -1,
    val exerciseIndex: Int = -1,
    val targetMuscle: String = "",
    val alternatives: List<GeneratedExercise> = emptyList(),
    val isVisible: Boolean = false
)

@HiltViewModel
class GeneratedPlanViewModel @Inject constructor(
    private val engine: WorkoutGeneratorEngine,
    private val workoutPlanDao: WorkoutPlanDao,
    private val workoutPlanExerciseDao: WorkoutPlanExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GeneratedPlanUiState>(GeneratedPlanUiState.Loading)
    val uiState: StateFlow<GeneratedPlanUiState> = _uiState

    private val _swapSheet = MutableStateFlow(SwapSheetState())
    val swapSheet: StateFlow<SwapSheetState> = _swapSheet

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private var lastInput: GenerationInput? = null
    private var savedPlanId: String? = null

    fun generate(input: GenerationInput) {
        lastInput = input
        viewModelScope.launch {
            _uiState.value = GeneratedPlanUiState.Loading
            runCatching { engine.generate(input) }
                .onSuccess { _uiState.value = GeneratedPlanUiState.Success(it) }
                .onFailure { _uiState.value = GeneratedPlanUiState.Error(it.message ?: "Generation failed") }
        }
    }

    fun regenerate() {
        lastInput?.let { generate(it) }
    }

    fun openSwapSheet(dayIndex: Int, exerciseIndex: Int) {
        val plan = (uiState.value as? GeneratedPlanUiState.Success)?.plan ?: return
        val exercise = plan.days.getOrNull(dayIndex)?.exercises?.getOrNull(exerciseIndex) ?: return
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val equipmentFilter = if (prefs.availableEquipment.isEmpty()) "" else "filtered"
            val equipmentList = prefs.availableEquipment.ifEmpty { emptyList() }
            val difficultyList = Difficulty.entries
                .filter {
                    it >= Difficulty.from(prefs.minDifficulty) &&
                    it <= Difficulty.from(prefs.maxDifficulty)
                }
                .map { it.name }

            val candidates = exerciseDao.getByMuscleGroupFiltered(
                group = exercise.targetMuscle,
                equipmentFilter = equipmentFilter,
                equipmentList = equipmentList,
                difficultyList = difficultyList
            ).filter { it.id != exercise.exerciseId }

            val alternatives = candidates.shuffled().take(6).map {
                GeneratedExercise(
                    exerciseId = it.id,
                    name = it.name,
                    targetMuscle = exercise.targetMuscle,
                    equipment = it.equipment,
                    sets = exercise.sets,
                    reps = exercise.reps,
                    youtubeVideoId = it.youtubeVideoId
                )
            }
            _swapSheet.value = SwapSheetState(
                dayIndex = dayIndex,
                exerciseIndex = exerciseIndex,
                targetMuscle = exercise.targetMuscle,
                alternatives = alternatives,
                isVisible = true
            )
        }
    }

    fun closeSwapSheet() {
        _swapSheet.value = _swapSheet.value.copy(isVisible = false)
    }

    fun swapExercise(replacement: GeneratedExercise) {
        val state = _uiState.value as? GeneratedPlanUiState.Success ?: return
        val sheet = _swapSheet.value
        val mutableDays = state.plan.days.toMutableList()
        val day = mutableDays[sheet.dayIndex]
        val mutableExercises = day.exercises.toMutableList()
        mutableExercises[sheet.exerciseIndex] = replacement
        mutableDays[sheet.dayIndex] = day.copy(exercises = mutableExercises)
        _uiState.value = state.copy(plan = state.plan.copy(days = mutableDays))
        closeSwapSheet()
    }

    fun savePlan(onSaved: (String) -> Unit) {
        if (_isSaving.value) return
        val state = _uiState.value as? GeneratedPlanUiState.Success ?: return
        val plan = state.plan
        viewModelScope.launch {
            _isSaving.value = true
            val planId = UUID.randomUUID().toString()
            val planName = "${plan.splitType.label} — ${plan.daysPerWeek} days"
            val entity = WorkoutPlanEntity(
                id = planId,
                name = planName,
                splitType = plan.splitType.name,
                daysPerWeek = plan.daysPerWeek,
                muscleGroups = plan.muscleGroups,
                createdAt = System.currentTimeMillis(),
                isTemplate = true
            )
            workoutPlanDao.insert(entity)

            val exercises = plan.days.flatMapIndexed { dayIdx, day ->
                day.exercises.mapIndexed { orderIdx, ex ->
                    WorkoutPlanExerciseEntity(
                        id = UUID.randomUUID().toString(),
                        planId = planId,
                        exerciseId = ex.exerciseId,
                        dayIndex = dayIdx,
                        orderIndex = orderIdx,
                        sets = ex.sets,
                        reps = ex.reps,
                        notes = ""
                    )
                }
            }
            workoutPlanExerciseDao.insertAll(exercises)
            savedPlanId = planId
            _uiState.value = GeneratedPlanUiState.Saved
            _isSaving.value = false
            onSaved(planId)
        }
    }
}
