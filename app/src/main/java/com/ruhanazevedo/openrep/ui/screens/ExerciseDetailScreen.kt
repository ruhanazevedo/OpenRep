package com.ruhanazevedo.openrep.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ruhanazevedo.openrep.ui.viewmodel.ExerciseDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onSearchYouTube: (String) -> Unit = {},
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exerciseImages by viewModel.exerciseImages.collectAsState()
    val remoteYoutubeId by viewModel.remoteYoutubeId.collectAsState()

    LaunchedEffect(uiState.deleteCompleted) {
        if (uiState.deleteCompleted) onBack()
    }

    if (uiState.isLoading) {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(64.dp))
                CircularProgressIndicator()
            }
        }
        return
    }

    val exercise = uiState.exercise ?: run {
        Scaffold { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Exercise not found")
            }
        }
        return
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete exercise") },
            text = {
                Text(
                    "Delete \"${exercise.name}\"? If this exercise appears in any saved plan, " +
                        "it will be marked as deleted but plan references will remain."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text("Cancel") }
            }
        )
    }

    var editableInstructions by remember(exercise.id) { mutableStateOf(exercise.instructions) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onSearchYouTube(exercise.id) }) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = "Link video")
                    }
                    if (exercise.isCustom) {
                        IconButton(onClick = { onEdit(exercise.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { viewModel.requestDelete() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
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
                .padding(16.dp)
        ) {
            DetailLabel("Difficulty")
            DifficultyBadge(exercise.difficulty.name)

            Spacer(Modifier.height(12.dp))

            DetailLabel("Muscle Groups")
            Text(exercise.muscleGroups.joinToString(", "))

            if (exercise.secondaryMuscleGroups.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                DetailLabel("Secondary Muscles")
                Text(exercise.secondaryMuscleGroups.joinToString(", "))
            }

            Spacer(Modifier.height(12.dp))

            DetailLabel("Equipment")
            Text(exercise.equipment)

            Spacer(Modifier.height(12.dp))

            DetailLabel("Instructions")
            OutlinedTextField(
                value = editableInstructions,
                onValueChange = { editableInstructions = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = exercise.isCustom
            )
            Spacer(Modifier.height(4.dp))
            if (exercise.isCustom) {
                TextButton(
                    onClick = { viewModel.saveInstructions(editableInstructions) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save instructions")
                }
            }

            val effectiveYoutubeId = exercise.youtubeVideoId ?: remoteYoutubeId
            val hasMedia = exerciseImages.isNotEmpty() || effectiveYoutubeId != null
            if (hasMedia) {
                Spacer(Modifier.height(16.dp))
                DetailLabel("Media")
                MediaCarousel(
                    images = exerciseImages,
                    youtubeVideoId = effectiveYoutubeId
                )
            }
        }
    }
}

@Composable
private fun MediaCarousel(images: List<String>, youtubeVideoId: String?) {
    val slides = buildList {
        addAll(images)
        if (youtubeVideoId != null) add(youtubeVideoId)
    }
    val imageCount = images.size
    val totalCount = slides.size

    if (totalCount == 0) return

    val pagerState = rememberPagerState(pageCount = { totalCount })
    val context = LocalContext.current

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            if (page < imageCount) {
                AsyncImage(
                    model = slides[page],
                    contentDescription = "Exercise image ${page + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color(0xFFE0E0E0))
                )
            } else {
                val videoId = youtubeVideoId ?: return@HorizontalPager
                val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = "YouTube thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/$videoId"))
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }
        }

        if (totalCount > 1) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalCount) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
}
