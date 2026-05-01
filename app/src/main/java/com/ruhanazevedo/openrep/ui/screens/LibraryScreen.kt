package com.ruhanazevedo.openrep.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruhanazevedo.openrep.domain.model.Difficulty
import com.ruhanazevedo.openrep.domain.model.Exercise
import com.ruhanazevedo.openrep.domain.model.ExerciseType
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("STRENGTH" to "Strength", "WARM_UP" to "Warm-up", "STRETCH" to "Stretch").forEach { (key, label) ->
                    FilterChip(
                        selected = key in uiState.selectedTypes,
                        onClick = { viewModel.toggleTypeFilter(key) },
                        label = { Text(label) }
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
    val difficultyColor = when (exercise.difficulty) {
        Difficulty.Beginner -> Color(0xFF4CAF50)
        Difficulty.Intermediate -> Color(0xFF00D4FF)
        Difficulty.Advanced -> Color(0xFFFF5252)
    }

    val primaryMuscle = exercise.muscleGroups.firstOrNull() ?: ""
    val avatarInitials = primaryMuscle
        .split(" ")
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .take(2)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Difficulty accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(difficultyColor)
            )

            Spacer(Modifier.width(12.dp))

            // Muscle group avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(difficultyColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitials,
                    style = MaterialTheme.typography.labelMedium,
                    color = difficultyColor
                )
            }

            Spacer(Modifier.width(12.dp))

            // Text content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        append(exercise.muscleGroups.joinToString(" • "))
                        if (exercise.equipment.isNotBlank()) append("  ·  ${exercise.equipment}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Difficulty label
            Text(
                text = exercise.difficulty.name,
                style = MaterialTheme.typography.labelSmall,
                color = difficultyColor,
                modifier = Modifier.padding(end = 16.dp)
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

@Composable
fun ExerciseTypeBadge(exerciseType: ExerciseType) {
    val (label, containerColor) = when (exerciseType) {
        ExerciseType.WARM_UP -> "Warm-up" to MaterialTheme.colorScheme.tertiaryContainer
        ExerciseType.STRETCH -> "Stretch" to MaterialTheme.colorScheme.secondaryContainer
        ExerciseType.STRENGTH -> return
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface) },
        colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor
        )
    )
}
