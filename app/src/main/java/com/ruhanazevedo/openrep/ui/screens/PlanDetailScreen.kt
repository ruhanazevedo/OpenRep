package com.ruhanazevedo.openrep.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.ruhanazevedo.openrep.ui.components.ShimmerList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruhanazevedo.openrep.ui.viewmodel.PlanDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: String,
    onStartWorkout: () -> Unit = {},
    onBack: () -> Unit = {},
    onExerciseDetail: (String) -> Unit = {},
    viewModel: PlanDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.plan?.plan?.name ?: "Plan Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (state.plan != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = onStartWorkout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Workout")
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ShimmerList(itemCount = 4, itemHeight = 88.dp)
                }
            }
            state.plan == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Plan not found.")
                }
            }
            else -> {
                val planWithExercises = state.plan!!
                val plan = planWithExercises.plan
                val byDay = planWithExercises.exercises.groupBy { it.dayIndex }.toSortedMap()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(plan.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "${plan.splitType} split • ${plan.daysPerWeek} days/week",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    byDay.forEach { (dayIdx, exercises) ->
                        item {
                            val muscles = exercises
                                .sortedBy { it.orderIndex }
                                .mapNotNull { state.exerciseDetails[it.id]?.targetMuscle }
                                .distinct()
                            val muscleSubtitle = if (muscles.size > 3) {
                                muscles.take(3).joinToString(" • ") + "..."
                            } else {
                                muscles.joinToString(" • ")
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Day ${dayIdx + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (muscleSubtitle.isNotEmpty()) {
                                        Text(
                                            muscleSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    exercises.sortedBy { it.orderIndex }.forEachIndexed { idx, ex ->
                                        if (idx > 0) HorizontalDivider()
                                        val detail = state.exerciseDetails[ex.id]
                                        val setsReps = "${ex.sets}×${ex.reps}"
                                        ListItem(
                                            modifier = Modifier.clickable { onExerciseDetail(ex.exerciseId) },
                                            headlineContent = {
                                                Text(detail?.exerciseName ?: ex.exerciseId)
                                            },
                                            overlineContent = {
                                                Text(detail?.targetMuscle ?: "")
                                            },
                                            supportingContent = {
                                                if (detail?.equipment != null) {
                                                    Text(
                                                        text = detail.equipment,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            trailingContent = {
                                                Text(
                                                    text = setsReps,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
