package com.ruhanazevedo.workoutgenerator.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutPlanEntity
import com.ruhanazevedo.workoutgenerator.ui.viewmodel.HistoryViewModel
import com.ruhanazevedo.workoutgenerator.ui.viewmodel.SessionSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onPlanClick: (String) -> Unit = {},
    onSessionDetailClick: (String) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Plans", "Sessions")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("History") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PlansTab(plans = uiState.plans, onPlanClick = onPlanClick)
                1 -> SessionsTab(sessions = uiState.sessions, onSessionClick = onSessionDetailClick)
            }
        }
    }
}

@Composable
private fun PlansTab(plans: List<WorkoutPlanEntity>, onPlanClick: (String) -> Unit) {
    if (plans.isEmpty()) {
        EmptyState(
            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.fillMaxSize()) },
            message = "No saved plans yet",
            subMessage = "Generate one to get started"
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { }
            items(plans) { plan ->
                PlanCard(plan = plan, onClick = { onPlanClick(plan.id) })
            }
        }
    }
}

@Composable
private fun SessionsTab(sessions: List<SessionSummary>, onSessionClick: (String) -> Unit) {
    if (sessions.isEmpty()) {
        EmptyState(
            icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.fillMaxSize()) },
            message = "No completed sessions yet",
            subMessage = "Start a workout to log your first session"
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { }
            items(sessions) { summary ->
                SessionCard(summary = summary, onClick = { onSessionClick(summary.session.id) })
            }
        }
    }
}

@Composable
private fun EmptyState(icon: @Composable () -> Unit, message: String, subMessage: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(64.dp)) { icon() }
            Text(message, style = MaterialTheme.typography.titleMedium)
            Text(
                subMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanCard(plan: WorkoutPlanEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(plan.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${plan.splitType} split • ${plan.daysPerWeek} days/week",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatDate(plan.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionCard(summary: SessionSummary, onClick: () -> Unit) {
    val session = summary.session
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(summary.planName, style = MaterialTheme.typography.titleMedium)
            Text(
                formatDate(session.startedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val durationStr = session.completedAt?.let { formatDuration(session.startedAt, it) }
            if (durationStr != null) {
                Text(
                    "$durationStr • ${summary.totalSets} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatDuration(startMs: Long, endMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(endMs - startMs)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
