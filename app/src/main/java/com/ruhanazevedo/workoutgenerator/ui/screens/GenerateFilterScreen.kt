package com.ruhanazevedo.workoutgenerator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruhanazevedo.workoutgenerator.domain.model.GenerationInput
import com.ruhanazevedo.workoutgenerator.domain.model.MuscleGroup
import com.ruhanazevedo.workoutgenerator.domain.model.SplitType
import com.ruhanazevedo.workoutgenerator.ui.viewmodel.GenerateFilterViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GenerateFilterScreen(
    onGenerate: (GenerationInput) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: GenerateFilterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val canGenerate = state.selectedMuscleGroups.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Days per week
            FilterSectionHeader("Days per Week")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..7).forEach { day ->
                    FilterChip(
                        selected = state.daysPerWeek == day,
                        onClick = { viewModel.setDaysPerWeek(day) },
                        label = { Text(day.toString()) }
                    )
                }
            }

            HorizontalDivider()

            // Muscle groups
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterSectionHeader("Muscle Groups")
                Row {
                    TextButton(onClick = { viewModel.selectAllMuscleGroups() }) { Text("All") }
                    TextButton(onClick = { viewModel.clearMuscleGroups() }) { Text("Clear") }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MuscleGroup.ALL.forEach { muscle ->
                    FilterChip(
                        selected = muscle in state.selectedMuscleGroups,
                        onClick = { viewModel.toggleMuscleGroup(muscle) },
                        label = { Text(muscle) }
                    )
                }
            }
            if (state.selectedMuscleGroups.isEmpty()) {
                Text(
                    "Select at least one muscle group to generate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider()

            // Training split
            FilterSectionHeader("Training Split")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SplitType.entries.forEach { split ->
                    val selected = state.splitType == split
                    OutlinedButton(
                        onClick = { viewModel.setSplitType(split) },
                        modifier = Modifier.fillMaxWidth(),
                        border = if (selected) {
                            androidx.compose.foundation.BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary
                            )
                        } else {
                            androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                split.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                split.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Generate button
            Button(
                onClick = { onGenerate(viewModel.buildGenerationInput()) },
                enabled = canGenerate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Generate")
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}
