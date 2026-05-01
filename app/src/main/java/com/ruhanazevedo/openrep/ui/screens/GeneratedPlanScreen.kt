package com.ruhanazevedo.openrep.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruhanazevedo.openrep.domain.model.GeneratedExercise
import com.ruhanazevedo.openrep.domain.model.GenerationInput
import com.ruhanazevedo.openrep.domain.model.WarmupCooldownItem
import com.ruhanazevedo.openrep.ui.viewmodel.GeneratedPlanUiState
import com.ruhanazevedo.openrep.ui.viewmodel.GeneratedPlanViewModel

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
                            val muscles = day.exercises
                                .map { it.targetMuscle }
                                .distinct()
                            val muscleSubtitle = if (muscles.size > 3) {
                                muscles.take(3).joinToString(" • ") + "..."
                            } else {
                                muscles.joinToString(" • ")
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                day.label,
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
                                        }
                                        if (day.estimatedMinutes > 0) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Timer,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(10.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    "~${day.estimatedMinutes} min",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (day.warmup.isNotEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        WarmupCooldownSection(
                                            title = "Warmup",
                                            items = day.warmup,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            icon = Icons.Default.LocalFireDepartment
                                        )
                                    }

                                    Spacer(Modifier.height(if (day.warmup.isNotEmpty()) 12.dp else 8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                                    Spacer(Modifier.height(4.dp))

                                    day.exercises.forEachIndexed { exIdx, exercise ->
                                        if (exIdx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                                        ExerciseRow(
                                            exercise = exercise,
                                            onSwap = { viewModel.openSwapSheet(dayIdx, exIdx) },
                                            onExerciseDetail = { onExerciseDetail(exercise.exerciseId) }
                                        )
                                    }

                                    if (day.cooldown.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                                        Spacer(Modifier.height(12.dp))
                                        WarmupCooldownSection(
                                            title = "Cooldown",
                                            items = day.cooldown,
                                            tint = MaterialTheme.colorScheme.primary,
                                            icon = Icons.Default.AcUnit
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
    onExerciseDetail: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onExerciseDetail() },
        headlineContent = { Text(exercise.name, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = exercise.equipment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    "${exercise.sets}×${exercise.reps}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        },
        overlineContent = { Text(exercise.targetMuscle) },
        trailingContent = {
            IconButton(onClick = onSwap) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Swap exercise",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

@Composable
private fun WarmupCooldownSection(
    title: String,
    items: List<WarmupCooldownItem>,
    tint: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = tint
            )
        }
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatSeconds(item.durationSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0 && s > 0) "${m}m ${s}s"
    else if (m > 0) "${m}m"
    else "${s}s"
}
