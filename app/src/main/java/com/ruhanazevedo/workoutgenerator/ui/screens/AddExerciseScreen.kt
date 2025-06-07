package com.ruhanazevedo.workoutgenerator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruhanazevedo.workoutgenerator.domain.model.Difficulty
import com.ruhanazevedo.workoutgenerator.domain.model.Equipment
import com.ruhanazevedo.workoutgenerator.domain.model.MuscleGroup
import com.ruhanazevedo.workoutgenerator.ui.viewmodel.AddExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExerciseScreen(
    onBack: () -> Unit = {},
    viewModel: AddExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) onBack()
    }

    val title = if (uiState.isEditMode) "Edit Exercise" else "Add Exercise"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.setName(it) },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                singleLine = true
            )

            FormSection(title = "Muscle Groups *") {
                uiState.muscleGroupError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MuscleGroup.ALL.forEach { muscle ->
                        FilterChip(
                            selected = muscle in uiState.muscleGroups,
                            onClick = { viewModel.toggleMuscleGroup(muscle) },
                            label = { Text(muscle) }
                        )
                    }
                }
            }

            FormSection(title = "Secondary Muscle Groups") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MuscleGroup.ALL.forEach { muscle ->
                        FilterChip(
                            selected = muscle in uiState.secondaryMuscleGroups,
                            onClick = { viewModel.toggleSecondaryMuscleGroup(muscle) },
                            label = { Text(muscle) }
                        )
                    }
                }
            }

            FormSection(title = "Equipment *") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Equipment.ALL.forEach { item ->
                        FilterChip(
                            selected = uiState.equipment == item,
                            onClick = { viewModel.setEquipment(item) },
                            label = { Text(item) }
                        )
                    }
                }
            }

            FormSection(title = "Difficulty *") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { d ->
                        FilterChip(
                            selected = uiState.difficulty == d.name,
                            onClick = { viewModel.setDifficulty(d.name) },
                            label = { Text(d.name) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.instructions,
                onValueChange = { viewModel.setInstructions(it) },
                label = { Text("Instructions") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = uiState.youtubeInput,
                onValueChange = { viewModel.setYoutubeInput(it) },
                label = { Text("YouTube URL or Video ID (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. dQw4w9WgXcQ or https://youtu.be/dQw4w9WgXcQ") }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isEditMode) "Save Changes" else "Add Exercise")
            }
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        content()
    }
}
