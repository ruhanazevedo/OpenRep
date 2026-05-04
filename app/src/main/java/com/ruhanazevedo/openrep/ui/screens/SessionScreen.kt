package com.ruhanazevedo.openrep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                title = {
                    Text(
                        if (state.planName.isNotBlank()) state.planName else "Workout",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.selectedDayIndex != null) {
                        Text(
                            formatElapsed(state.elapsedSeconds),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
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
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Workout Complete",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard("Time", formatElapsed(state.elapsedSeconds), modifier = Modifier.weight(1f))
                        StatCard("Done", "$completedCount/${exercises.size}", modifier = Modifier.weight(1f))
                        StatCard("Sets", "$totalSets", modifier = Modifier.weight(1f))
                    }

                    if (totalVolume > 0f) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(0.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Total Volume",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${"%.1f".format(totalVolume)} kg",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (exercises.isNotEmpty()) {
                        Text(
                            "Exercise Summary",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        exercises.forEach { exercise ->
                            val sets = state.exerciseStates[exercise.planExercise.id]?.loggedSets ?: emptyList()
                            if (sets.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(0.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(exercise.exerciseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        sets.forEach { set ->
                                            val w = if ((set.weightKg ?: 0f) > 0f) " · ${set.weightKg}kg" else ""
                                            Text(
                                                "Set ${set.setNumber}  ·  ${set.repsCompleted} reps$w",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.dismissSummary() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Done") }

                    Spacer(Modifier.height(16.dp))
                }
            }

            state.days.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { Text("No exercises in this plan.") }
            }

            state.selectedDayIndex == null -> {
                // ── Day selection ──────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Select Day",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Choose today's training split",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    state.days.forEach { day ->
                        val recentlyDone = day.lastDoneAt != null &&
                            (System.currentTimeMillis() - day.lastDoneAt) < 24 * 60 * 60 * 1000L

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clickable { viewModel.selectDay(day.dayIndex) },
                            elevation = CardDefaults.cardElevation(0.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                        .background(
                                            if (recentlyDone) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.primary
                                        )
                                )
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(day.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    if (day.muscleGroups.isNotBlank()) {
                                        Text(
                                            day.muscleGroups,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
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
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (day.lastDoneAt == null) MaterialTheme.colorScheme.onSurfaceVariant
                                                else MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            else -> {
                // ── Active workout ──────────────────────────────────────
                val selectedDay = state.days.find { it.dayIndex == state.selectedDayIndex }
                if (selectedDay == null) return@Scaffold

                val exercises = selectedDay.exercises
                val completedCount = exercises.count { ex ->
                    (state.exerciseStates[ex.planExercise.id] ?: ExerciseSessionState()).loggedSets.size >= ex.setsTarget
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Progress header ───────────────────────────────
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                selectedDay.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "$completedCount of ${exercises.size} done",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (exercises.isNotEmpty()) completedCount.toFloat() / exercises.size else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }

                    // ── Rest timer banner ─────────────────────────────
                    if (state.restTimerRunning) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "REST",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${state.restTimerSeconds}s",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(onClick = { viewModel.skipRest() }) {
                                    Text("Skip Rest")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Exercise cards ────────────────────────────────
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        exercises.forEach { exercise ->
                            val planExerciseId = exercise.planExercise.id
                            val exState = state.exerciseStates[planExerciseId] ?: ExerciseSessionState()
                            val isCompleted = exState.loggedSets.size >= exercise.setsTarget
                            val hasStarted = exState.loggedSets.isNotEmpty()

                            val accentColor = when {
                                isCompleted -> MaterialTheme.colorScheme.tertiary
                                hasStarted  -> MaterialTheme.colorScheme.primary
                                else        -> MaterialTheme.colorScheme.surfaceContainerHigh
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(0.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                    // Left accent bar
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                            .background(accentColor)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)
                                    ) {
                                        // Header: name + status badge
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    exercise.exerciseName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (exercise.targetMuscle.isNotBlank()) {
                                                    Text(
                                                        exercise.targetMuscle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                            }
                                            if (isCompleted) {
                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        "Done",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    "${exercise.setsTarget} sets × ${exercise.repsTarget} reps",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // ── STRENGTH exercise ─────────────────────
                                        if (exercise.exerciseType == ExerciseType.STRENGTH) {

                                            // Logged set rows
                                            if (exState.loggedSets.isNotEmpty()) {
                                                Spacer(Modifier.height(10.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                                                Spacer(Modifier.height(6.dp))
                                                exState.loggedSets.forEach { set ->
                                                    val weightStr = if ((set.weightKg ?: 0f) > 0f) " · ${set.weightKg}kg" else ""
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 3.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.CheckCircle,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                "Set ${set.setNumber}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        Text(
                                                            "${set.repsCompleted} reps$weightStr",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }

                                            // Active set input (when not done)
                                            if (!isCompleted) {
                                                Spacer(Modifier.height(12.dp))
                                                Text(
                                                    "Set ${exState.currentSetNumber} of ${exercise.setsTarget}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                                        label = { Text("kg") },
                                                        modifier = Modifier.weight(1f),
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        singleLine = true,
                                                        placeholder = { Text("optional") }
                                                    )
                                                }
                                                Spacer(Modifier.height(10.dp))
                                                Button(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.logSet(planExerciseId)
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = exState.repsInput.trim().toIntOrNull() != null
                                                ) {
                                                    Text(
                                                        if (exState.currentSetNumber == exercise.setsTarget) "Log Last Set"
                                                        else "Log Set ${exState.currentSetNumber}"
                                                    )
                                                }
                                            }

                                        } else {
                                            // ── TIMED exercise ────────────────────
                                            val duration = exercise.durationSeconds ?: 60
                                            when {
                                                exState.timerRunning -> {
                                                    Spacer(Modifier.height(10.dp))
                                                    Text(
                                                        "${exState.remainingSeconds}s",
                                                        style = MaterialTheme.typography.displaySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(Modifier.height(8.dp))
                                                    OutlinedButton(
                                                        onClick = { viewModel.stopExerciseTimer(planExerciseId) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) { Text("Pause") }
                                                }
                                                !isCompleted -> {
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        "${duration}s · timed exercise",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(Modifier.height(8.dp))
                                                    Button(
                                                        onClick = { viewModel.startExerciseTimer(planExerciseId) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) { Text("Start Timer") }
                                                }
                                                else -> { /* Done badge in header covers this */ }
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

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
