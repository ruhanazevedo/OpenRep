package com.ruhanazevedo.openrep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruhanazevedo.openrep.ui.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    planId: String,
    onFinish: () -> Unit = {},
    onBack: () -> Unit = {},
    onExerciseDetail: (String) -> Unit = {},
    viewModel: SessionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val exercisesDone = state.currentExerciseIndex >= state.exercises.size

    LaunchedEffect(state.finishCompleted) {
        if (state.finishCompleted) onFinish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.planName.isNotBlank()) state.planName else "Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!exercisesDone && state.exercises.isNotEmpty()) {
                        IconButton(onClick = { onExerciseDetail(state.exercises[state.currentExerciseIndex].planExercise.exerciseId) }) {
                            Icon(Icons.Default.Info, contentDescription = "Exercise info")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            state.exercises.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { Text("No exercises in this plan.") }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress bar
                    val progress = if (state.exercises.isNotEmpty()) {
                        state.currentExerciseIndex.toFloat() / state.exercises.size.toFloat()
                    } else 1f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${state.currentExerciseIndex}/${state.exercises.size} exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (exercisesDone) {
                        // All sets done — show finish
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("All exercises complete!", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${state.loggedSets.size} sets logged",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.finishWorkout() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Finish Workout")
                        }
                    } else {
                        val currentExercise = state.exercises[state.currentExerciseIndex]

                        // Current exercise card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(currentExercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                                if (currentExercise.targetMuscle.isNotBlank()) {
                                    Text(
                                        currentExercise.targetMuscle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "Target: ${currentExercise.setsTarget} sets × ${currentExercise.repsTarget} reps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Set ${state.currentSetNumber} of ${currentExercise.setsTarget}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (currentExercise.instructions.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        currentExercise.instructions,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Rest timer
                        if (state.restTimerRunning) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Rest: ${state.restTimerSeconds}s",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    OutlinedButton(onClick = { viewModel.skipRest() }) {
                                        Text("Skip")
                                    }
                                }
                            }
                        }

                        // Set input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = state.repsInput,
                                onValueChange = { viewModel.setRepsInput(it) },
                                label = { Text("Reps") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                placeholder = { Text("${currentExercise.repsTarget}") }
                            )
                            Spacer(Modifier.width(0.dp))
                            OutlinedTextField(
                                value = state.weightInput,
                                onValueChange = { viewModel.setWeightInput(it) },
                                label = { Text("Weight (kg)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                placeholder = { Text("optional") }
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.logSet()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.repsInput.trim().toIntOrNull() != null
                        ) {
                            Text("Log Set")
                        }

                        // Logged sets for current exercise
                        val currentPlanExerciseId = currentExercise.planExercise.id
                        val logsForCurrent = state.loggedSets.filter { it.planExerciseId == currentPlanExerciseId }
                        if (logsForCurrent.isNotEmpty()) {
                            Text("Logged sets:", style = MaterialTheme.typography.labelMedium)
                            logsForCurrent.forEach { set ->
                                val weightStr = if ((set.weightKg ?: 0f) > 0f) "${set.weightKg}kg" else "bodyweight"
                                Text(
                                    "Set ${set.setNumber}: ${set.repsCompleted} reps @ $weightStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.finishWorkout() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Finish Workout Early")
                        }
                    }
                }
            }
        }
    }
}
