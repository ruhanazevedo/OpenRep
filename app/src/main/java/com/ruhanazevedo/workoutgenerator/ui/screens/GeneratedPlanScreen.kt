package com.ruhanazevedo.workoutgenerator.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruhanazevedo.workoutgenerator.domain.model.GeneratedExercise
import com.ruhanazevedo.workoutgenerator.domain.model.GenerationInput
import com.ruhanazevedo.workoutgenerator.ui.viewmodel.GeneratedPlanUiState
import com.ruhanazevedo.workoutgenerator.ui.viewmodel.GeneratedPlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedPlanScreen(
    input: GenerationInput? = null,
    generationTrigger: Int = 0,
    onSave: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onExerciseDetail: (String) -> Unit = {},
    viewModel: GeneratedPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val swapSheet by viewModel.swapSheet.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(generationTrigger) {
        if (input != null) viewModel.generate(input)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generated Plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.regenerate() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is GeneratedPlanUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is GeneratedPlanUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GeneratedPlanUiState.Success -> {
                val plan = state.plan
                if (plan.days.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No exercises found for the selected filters.")
                            Text(
                                "Try adjusting muscle groups or equipment in Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(plan.days) { dayIdx, day ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        day.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    day.exercises.forEachIndexed { exIdx, exercise ->
                                        if (exIdx > 0) HorizontalDivider()
                                        ExerciseRow(
                                            exercise = exercise,
                                            onSwap = { viewModel.openSwapSheet(dayIdx, exIdx) },
                                            onVideoClick = { exercise.youtubeVideoId?.let { onExerciseDetail(exercise.exerciseId) } }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Button(
                                onClick = { viewModel.savePlan(onSave) },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(if (isSaving) "Saving…" else "Save Plan")
                            }
                        }
                    }
                }
            }
            is GeneratedPlanUiState.Saved -> {
                // Navigation handled via callback; show nothing extra
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (swapSheet.isVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSwapSheet() },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Swap Exercise — ${swapSheet.targetMuscle}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                if (swapSheet.alternatives.isEmpty()) {
                    Text(
                        "No alternative exercises found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    swapSheet.alternatives.forEach { alt ->
                        ListItem(
                            headlineContent = { Text(alt.name) },
                            supportingContent = {
                                Text("${alt.equipment} • ${alt.sets}×${alt.reps}")
                            },
                            trailingContent = {
                                IconButton(onClick = { viewModel.swapExercise(alt) }) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Select")
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: GeneratedExercise,
    onSwap: () -> Unit,
    onVideoClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(exercise.name, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(exercise.equipment) })
                Text(
                    "${exercise.sets}×${exercise.reps}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        },
        overlineContent = { Text(exercise.targetMuscle) },
        trailingContent = {
            Row {
                if (exercise.youtubeVideoId != null) {
                    IconButton(onClick = onVideoClick) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = "Watch video",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onSwap) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Swap exercise",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}
