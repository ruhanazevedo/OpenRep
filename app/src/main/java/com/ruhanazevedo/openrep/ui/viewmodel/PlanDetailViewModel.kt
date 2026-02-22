package com.ruhanazevedo.openrep.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.openrep.data.db.dao.ExerciseDao
import com.ruhanazevedo.openrep.data.db.dao.WorkoutPlanDao
import com.ruhanazevedo.openrep.data.db.entity.WorkoutPlanExerciseEntity
import com.ruhanazevedo.openrep.data.db.entity.WorkoutPlanWithExercises
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PlanExerciseDisplay(
    val entity: WorkoutPlanExerciseEntity,
    val exerciseName: String,
    val targetMuscle: String,
    val equipment: String,
    val youtubeVideoId: String?
)

data class PlanDetailUiState(
    val plan: WorkoutPlanWithExercises? = null,
    val exerciseDetails: Map<String, PlanExerciseDisplay> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PlanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutPlanDao: WorkoutPlanDao,
    private val exerciseDao: ExerciseDao
) : ViewModel() {

    private val planId: String = savedStateHandle["planId"] ?: ""

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlanDetailUiState> = workoutPlanDao.getPlanWithExercises(planId)
        .flatMapLatest { planWithExercises ->
            if (planWithExercises == null) {
                flowOf(PlanDetailUiState(isLoading = false))
            } else {
                flow {
                    val details = planWithExercises.exercises.associate { ex ->
                        val entity = exerciseDao.getById(ex.exerciseId).firstOrNull()
                        ex.id to PlanExerciseDisplay(
                            entity = ex,
                            exerciseName = entity?.name ?: "Unknown",
                            targetMuscle = entity?.muscleGroups?.firstOrNull() ?: "",
                            equipment = entity?.equipment ?: "",
                            youtubeVideoId = entity?.youtubeVideoId
                        )
                    }
                    emit(PlanDetailUiState(plan = planWithExercises, exerciseDetails = details, isLoading = false))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanDetailUiState()
        )
}
