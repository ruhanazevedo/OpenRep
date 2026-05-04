package com.ruhanazevedo.openrep.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruhanazevedo.openrep.domain.model.ExerciseType
import com.ruhanazevedo.openrep.ui.viewmodel.ExerciseSessionState
import com.ruhanazevedo.openrep.ui.viewmodel.SessionViewModel

private fun formatElapsed(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

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
                    if (state.selectedDayIndex != null) {
                        Text(
                            formatElapsed(state.elapsedSeconds),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 16.dp)
                        )
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
            state.showSummary -> {
                val selectedDay = state.days.find { it.dayIndex == state.selectedDayIndex }
                val exercises = selectedDay?.exercises ?: emptyList()
                val completedCount = exercises.count { ex ->
                    (state.exerciseStates[ex.planExercise.id] ?: ExerciseSessionState()).loggedSets.size >= ex.setsTarget
                }
                val totalSets = state.exerciseStates.values.sumOf { it.loggedSets.size }
                val totalVolume = state.exerciseStates.values
                    .flatMap { it.loggedSets }
                    .filter { (it.weightKg ?: 0f) > 0f }
                    .sumOf { (it.repsCompleted * (it.weightKg ?: 0f)).toDouble() }
                    .toFloat()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Workout Complete!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard("Time", formatElapsed(state.elapsedSeconds))
                        StatCard("Exercises", "$completedCount/${exercises.size}")
                        StatCard("Sets", "$totalSets")
                    }

                    if (totalVolume > 0f) {
                        Text(
                            "Total volume: ${"%.1f".format(totalVolume)}kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    if (exercises.isNotEmpty()) {
                        Text(
                            "Exercises",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        exercises.forEach { exercise ->
                            val sets = state.exerciseStates[exercise.planExercise.id]?.loggedSets ?: emptyList()
                            if (sets.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(exercise.exerciseName, style = MaterialTheme.typography.titleSmall)
                                    sets.forEach { set ->
                                        val w = if ((set.weightKg ?: 0f) > 0f) " @ ${set.weightKg}kg" else ""
                                        Text(
                                            "  Set ${set.setNumber}: ${set.repsCompleted} reps$w",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.dismissSummary() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }
            }
            state.days.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { Text("No exercises in this plan.") }
            }
            state.selectedDayIndex == null -> {
                // Day selection view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Select today's training",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                    state.days.forEach { day ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { viewModel.selectDay(day.dayIndex) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(day.label, style = MaterialTheme.typography.titleMedium)
                                if (day.muscleGroups.isNotBlank()) {
                                    Text(
                                        day.muscleGroups,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val lastDoneText = when {
                                    day.lastDoneAt == null -> "Never done"
                                    else -> {
                                        val daysAgo = ((System.currentTimeMillis() - day.lastDoneAt) / (1000 * 60 * 60 * 24)).toInt()
                                        when (daysAgo) {
                                            0 -> "Done today"
                                            1 -> "Done yesterday"
                                            else -> "Done $daysAgo days ago"
                                        }
                                    }
                                }
                                Text(
                                    lastDoneText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (day.lastDoneAt == null) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            else -> {
                // Exercise list view
                val selectedDay = state.days.find { it.dayIndex == state.selectedDayIndex }
                if (selectedDay == null) return@Scaffold

                val exercises = selectedDay.exercises
                val completedCount = exercises.count { ex ->
                    val exState = state.exerciseStates[ex.planExercise.id] ?: ExerciseSessionState()
                    exState.loggedSets.size >= ex.setsTarget
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Progress
                    LinearProgressIndicator(
                        progress = { if (exercises.isNotEmpty()) completedCount.toFloat() / exercises.size else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "$completedCount/${exercises.size} exercises complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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

                    // Exercise cards
                    exercises.forEach { exercise ->
                        val planExerciseId = exercise.planExercise.id
                        val exState = state.exerciseStates[planExerciseId] ?: ExerciseSessionState()
                        val isCompleted = exState.loggedSets.size >= exercise.setsTarget

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column {
                                // ── Header row (tap to expand) ─────────────────────
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleExpand(planExerciseId) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Completion indicator / set count circle
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceContainerHigh
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Text(
                                                "${exState.loggedSets.size}/${exercise.setsTarget}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            exercise.exerciseName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${exercise.setsTarget} sets × ${exercise.repsTarget} reps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        if (exState.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // ── Expanded: set logging ──────────────────────────
                                AnimatedVisibility(visible = exState.isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

                                        if (exercise.exerciseType == ExerciseType.STRENGTH) {
                                            if (isCompleted) {
                                                // Logged sets summary
                                                exState.loggedSets.forEach { set ->
                                                    val weightStr = if ((set.weightKg ?: 0f) > 0f) " @ ${set.weightKg}kg" else ""
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            "Set ${set.setNumber}",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            "${set.repsCompleted} reps$weightStr",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            } else {
                                                // Current set prompt
                                                Text(
                                                    "Set ${exState.currentSetNumber} of ${exercise.setsTarget}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = exState.repsInput,
                                                        onValueChange = { viewModel.setRepsInput(planExerciseId, it) },
                                                        label = { Text("Reps") },
                                                        modifier = Modifier.weight(1f),
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true,
                                                        placeholder = { Text("${exercise.repsTarget}") }
                                                    )
                                                    OutlinedTextField(
                                                        value = exState.weightInput,
                                                        onValueChange = { viewModel.setWeightInput(planExerciseId, it) },
                                                        label = { Text("Weight (kg)") },
                                                        modifier = Modifier.weight(1f),
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        singleLine = true,
                                                        placeholder = { Text("optional") }
                                                    )
                                                }

                                                // Previous logged sets (compact)
                                                if (exState.loggedSets.isNotEmpty()) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        exState.loggedSets.forEach { set ->
                                                            val weightStr = if ((set.weightKg ?: 0f) > 0f) " @ ${set.weightKg}kg" else ""
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    "Set ${set.setNumber}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                Text(
                                                                    "${set.repsCompleted} reps$weightStr",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.logSet(planExerciseId)
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = exState.repsInput.trim().toIntOrNull() != null
                                                ) {
                                                    Text("Log Set ${exState.currentSetNumber}")
                                                }
                                            }
                                        } else {
                                            // Timed exercise
                                            val duration = exercise.durationSeconds ?: 60
                                            when {
                                                exState.timerRunning -> {
                                                    Text(
                                                        "${exState.remainingSeconds}s remaining",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    OutlinedButton(
                                                        onClick = { viewModel.stopExerciseTimer(planExerciseId) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) { Text("Pause") }
                                                }
                                                isCompleted -> {
                                                    Text(
                                                        "Completed",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                else -> {
                                                    Text(
                                                        "Duration: ${duration}s",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Button(
                                                        onClick = { viewModel.startExerciseTimer(planExerciseId) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) { Text("Start Timer") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
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
                        Text("Finish Workout")
                    }
                }
            }
        }
    }
}
