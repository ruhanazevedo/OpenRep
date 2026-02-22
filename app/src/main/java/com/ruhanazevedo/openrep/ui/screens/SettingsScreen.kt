package com.ruhanazevedo.openrep.ui.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruhanazevedo.openrep.domain.model.Difficulty
import com.ruhanazevedo.openrep.domain.model.Equipment
import com.ruhanazevedo.openrep.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
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
            // ── Rest timer ──────────────────────────────────────────────
            SectionHeader("Rest Timer")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Duration", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${prefs.restTimerSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = prefs.restTimerSeconds.toFloat(),
                    onValueChange = { viewModel.setRestTimerSeconds(it.toInt()) },
                    valueRange = 15f..300f,
                    steps = 18, // 15s increments
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("15s", style = MaterialTheme.typography.labelSmall)
                    Text("5 min", style = MaterialTheme.typography.labelSmall)
                }
            }

            HorizontalDivider()

            // ── Equipment ────────────────────────────────────────────────
            SectionHeader("Available Equipment")
            Text(
                "Select all equipment you have access to. Leave all selected for no restriction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val selectedEquipment = if (prefs.availableEquipment.isEmpty()) {
                Equipment.ALL.toSet()
            } else {
                prefs.availableEquipment.toSet()
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Equipment.ALL.forEach { item ->
                    val selected = item in selectedEquipment
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val newSet = if (selected) {
                                selectedEquipment - item
                            } else {
                                selectedEquipment + item
                            }
                            // empty list means "all"
                            val newList = if (newSet.size == Equipment.ALL.size) emptyList()
                            else newSet.toList()
                            viewModel.setAvailableEquipment(newList)
                        },
                        label = { Text(item) }
                    )
                }
            }

            HorizontalDivider()

            // ── Difficulty range ─────────────────────────────────────────
            SectionHeader("Preferred Difficulty Range")
            val difficulties = Difficulty.entries
            val minIdx = difficulties.indexOfFirst {
                it.name.equals(prefs.minDifficulty, ignoreCase = true)
            }.coerceAtLeast(0)
            val maxIdx = difficulties.indexOfFirst {
                it.name.equals(prefs.maxDifficulty, ignoreCase = true)
            }.coerceIn(0, difficulties.lastIndex)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Min", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        difficulties[minIdx].name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = minIdx.toFloat(),
                    onValueChange = { idx ->
                        val newMin = difficulties[idx.toInt()]
                        val currentMax = difficulties[maxIdx]
                        if (newMin <= currentMax) {
                            viewModel.setMinDifficulty(newMin.name)
                        }
                    },
                    valueRange = 0f..difficulties.lastIndex.toFloat(),
                    steps = difficulties.lastIndex - 1,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Max", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        difficulties[maxIdx].name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = maxIdx.toFloat(),
                    onValueChange = { idx ->
                        val newMax = difficulties[idx.toInt()]
                        val currentMin = difficulties[minIdx]
                        if (newMax >= currentMin) {
                            viewModel.setMaxDifficulty(newMax.name)
                        }
                    },
                    valueRange = 0f..difficulties.lastIndex.toFloat(),
                    steps = difficulties.lastIndex - 1,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    difficulties.forEach { d ->
                        Text(d.name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider()

            // ── Exercises per muscle group ────────────────────────────────
            SectionHeader("Exercises per Muscle Group")
            Text(
                "Number of exercises selected per muscle group when generating a plan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            var exercisesInput by remember(prefs.exercisesPerMuscleGroup) {
                mutableStateOf(prefs.exercisesPerMuscleGroup.toString())
            }
            OutlinedTextField(
                value = exercisesInput,
                onValueChange = { raw ->
                    exercisesInput = raw
                    val parsed = raw.toIntOrNull()
                    if (parsed != null && parsed in 1..10) {
                        viewModel.setExercisesPerMuscleGroup(parsed)
                    }
                },
                label = { Text("Count (1–10)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = exercisesInput.toIntOrNull()?.let { it !in 1..10 } ?: true,
                supportingText = {
                    val v = exercisesInput.toIntOrNull()
                    if (v == null || v !in 1..10) {
                        Text("Enter a value between 1 and 10")
                    }
                }
            )

            // Bottom padding so last item isn't flush with nav bar
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}
