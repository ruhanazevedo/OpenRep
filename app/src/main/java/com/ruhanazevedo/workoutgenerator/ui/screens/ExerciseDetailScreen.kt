package com.ruhanazevedo.workoutgenerator.ui.screens

import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruhanazevedo.workoutgenerator.ui.viewmodel.ExerciseDetailViewModel

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

            exercise.youtubeVideoId?.let { videoId ->
                Spacer(Modifier.height(16.dp))
                DetailLabel("Video")
                YouTubeWebView(videoId = videoId)
            }
        }
    }
}

@Composable
private fun DetailLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun YouTubeWebView(videoId: String) {
    val html = """
        <html>
        <body style="margin:0;padding:0;background:#000;">
        <iframe width="100%" height="100%"
            src="https://www.youtube.com/embed/$videoId?autoplay=0"
            frameborder="0"
            allowfullscreen>
        </iframe>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                            v.parent.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP ->
                            v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}
