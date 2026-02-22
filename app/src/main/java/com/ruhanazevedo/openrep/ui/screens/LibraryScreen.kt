package com.ruhanazevedo.openrep.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruhanazevedo.openrep.domain.model.Exercise
import com.ruhanazevedo.openrep.domain.model.MuscleGroup
import com.ruhanazevedo.openrep.ui.components.ShimmerList
import com.ruhanazevedo.openrep.ui.viewmodel.ImportDuplicatePrompt
import com.ruhanazevedo.openrep.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onExerciseClick: (String) -> Unit = {},
    onAddExerciseClick: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importFromUri(context, uri)
    }

    if (uiState.importResult != null) {
        val result = uiState.importResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportResult() },
            title = { Text("Import complete") },
            text = {
                Column {
                    Text("${result.imported} imported, ${result.skipped} skipped")
                    if (result.errors.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        result.errors.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissImportResult() }) { Text("OK") }
            }
        )
    }

    val duplicatePrompt = uiState.duplicatePrompt
    if (duplicatePrompt is ImportDuplicatePrompt.Pending) {
        AlertDialog(
            onDismissRequest = { viewModel.resolveImportDuplicate(false) },
            title = { Text("Duplicate exercise") },
            text = { Text("\"${duplicatePrompt.entry.name}\" already exists. Overwrite it?") },
            confirmButton = {
                TextButton(onClick = { viewModel.resolveImportDuplicate(true) }) { Text("Overwrite") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resolveImportDuplicate(false) }) { Text("Skip") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Library") },
                actions = {
                    IconButton(onClick = { filePicker.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Default.Upload, contentDescription = "Import from file")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExerciseClick) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search exercises…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MuscleGroup.ALL.forEach { muscle ->
                    FilterChip(
                        selected = muscle in uiState.selectedMuscleGroups,
                        onClick = { viewModel.toggleMuscleGroup(muscle) },
                        label = { Text(muscle) }
                    )
                }
            }

            when {
                uiState.isLoading -> ShimmerList()
                uiState.exercises.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null)
                            Text("No exercises found", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.exercises, key = { it.id }) { exercise ->
                            ExerciseListItem(
                                exercise = exercise,
                                onClick = { onExerciseClick(exercise.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseListItem(exercise: Exercise, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                DifficultyBadge(difficulty = exercise.difficulty.name)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = exercise.muscleGroups.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = exercise.equipment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: String) {
    val color = when (difficulty) {
        "Beginner" -> MaterialTheme.colorScheme.tertiary
        "Intermediate" -> MaterialTheme.colorScheme.primary
        "Advanced" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    SuggestionChip(
        onClick = {},
        label = { Text(difficulty, style = MaterialTheme.typography.labelSmall, color = color) }
    )
}
