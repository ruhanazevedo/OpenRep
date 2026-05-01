package com.ruhanazevedo.openrep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            SettingsSectionLabel("Appearance")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (prefs.isDarkMode) "Dark theme enabled" else "Light theme enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            SettingsSectionLabel("Rest Timer")
            SettingsCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("15s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("5 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            SettingsSectionLabel("Available Equipment")
            SettingsCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                    val newSet = if (selected) selectedEquipment - item else selectedEquipment + item
                                    val newList = if (newSet.size == Equipment.ALL.size) emptyList() else newSet.toList()
                                    viewModel.setAvailableEquipment(newList)
                                },
                                label = { Text(item) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            SettingsSectionLabel("Preferred Difficulty Range")
            SettingsCard {
                val difficulties = Difficulty.entries
                val minIdx = difficulties.indexOfFirst {
                    it.name.equals(prefs.minDifficulty, ignoreCase = true)
                }.coerceAtLeast(0)
                val maxIdx = difficulties.indexOfFirst {
                    it.name.equals(prefs.maxDifficulty, ignoreCase = true)
                }.coerceIn(0, difficulties.lastIndex)

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Min", style = MaterialTheme.typography.bodyLarge)
                        Text(difficulties[minIdx].name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = minIdx.toFloat(),
                        onValueChange = { idx ->
                            val newMin = difficulties[idx.toInt()]
                            if (newMin <= difficulties[maxIdx]) viewModel.setMinDifficulty(newMin.name)
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
                        Text(difficulties[maxIdx].name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = maxIdx.toFloat(),
                        onValueChange = { idx ->
                            val newMax = difficulties[idx.toInt()]
                            if (newMax >= difficulties[minIdx]) viewModel.setMaxDifficulty(newMax.name)
                        },
                        valueRange = 0f..difficulties.lastIndex.toFloat(),
                        steps = difficulties.lastIndex - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        difficulties.forEach { d ->
                            Text(d.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            SettingsSectionLabel("Exercises per Muscle Group")
            SettingsCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            if (parsed != null && parsed in 1..10) viewModel.setExercisesPerMuscleGroup(parsed)
                        },
                        label = { Text("Count (1-10)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = exercisesInput.toIntOrNull()?.let { it !in 1..10 } ?: true,
                        supportingText = {
                            val v = exercisesInput.toIntOrNull()
                            if (v == null || v !in 1..10) Text("Enter a value between 1 and 10")
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        content()
    }
}
