package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.data.db.dao.SessionSetDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutPlanDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutSessionDao
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutPlanEntity
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SessionSummary(
    val session: WorkoutSessionEntity,
    val planName: String,
    val totalSets: Int
)

data class HistoryUiState(
    val plans: List<WorkoutPlanEntity> = emptyList(),
    val sessions: List<SessionSummary> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    workoutPlanDao: WorkoutPlanDao,
    workoutSessionDao: WorkoutSessionDao,
    private val sessionSetDao: SessionSetDao
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = combine(
        workoutPlanDao.getAll(),
        workoutSessionDao.getAll()
    ) { plans, sessions -> plans to sessions }
        .flatMapLatest { (plans, sessions) ->
            flow {
                val completedSessions = sessions.filter { it.completedAt != null }
                val planMap = plans.associateBy { it.id }
                val summaries = completedSessions.map { session ->
                    SessionSummary(
                        session = session,
                        planName = planMap[session.planId]?.name ?: "Deleted plan",
                        totalSets = sessionSetDao.countBySessionId(session.id)
                    )
                }
                emit(HistoryUiState(plans = plans, sessions = summaries))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )
}
