package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.data.db.dao.ExerciseDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.SessionSetDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutPlanDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutPlanExerciseDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutSessionDao
import com.ruhanazevedo.workoutgenerator.data.db.entity.SessionSetEntity
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SetDisplay(
    val setNumber: Int,
    val repsCompleted: Int,
    val weightKg: Float?
)

data class ExerciseSetGroup(
    val exerciseName: String,
    val sets: List<SetDisplay>
)

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val session: WorkoutSessionEntity? = null,
    val planName: String = "",
    val groups: List<ExerciseSetGroup> = emptyList()
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao,
    private val sessionSetDao: SessionSetDao,
    private val workoutPlanExerciseDao: WorkoutPlanExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val workoutPlanDao: WorkoutPlanDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SessionDetailUiState> = workoutSessionDao.getById(sessionId)
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(SessionDetailUiState(isLoading = false))
            } else {
                sessionSetDao.getBySessionId(sessionId).flatMapLatest { sets ->
                    flow {
                        val planName = session.planId?.let {
                            workoutPlanDao.getById(it).firstOrNull()?.name
                        } ?: "Deleted plan"

                        val groups = buildGroups(sets)
                        emit(
                            SessionDetailUiState(
                                isLoading = false,
                                session = session,
                                planName = planName,
                                groups = groups
                            )
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionDetailUiState()
        )

    private suspend fun buildGroups(sets: List<SessionSetEntity>): List<ExerciseSetGroup> {
        val byPlanExercise = sets.groupBy { it.planExerciseId }
        return byPlanExercise.mapNotNull { (planExerciseId, setSets) ->
            if (planExerciseId == null) return@mapNotNull null
            val planExercise = workoutPlanExerciseDao.getById(planExerciseId) ?: return@mapNotNull null
            val exerciseName = exerciseDao.getById(planExercise.exerciseId).firstOrNull()?.name ?: "Unknown"
            ExerciseSetGroup(
                exerciseName = exerciseName,
                sets = setSets.sortedBy { it.setNumber }.map { s ->
                    SetDisplay(s.setNumber, s.repsCompleted, s.weightKg)
                }
            )
        }
    }
}
