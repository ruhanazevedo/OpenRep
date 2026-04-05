package com.ruhanazevedo.openrep.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

val bottomNavItems = listOf(
    BottomNavItem("Workout", Icons.Default.History, Screen.History),
    BottomNavItem("Library", Icons.Default.List, Screen.Library),
    BottomNavItem("Generate", Icons.Default.FitnessCenter, Screen.Generate),
    BottomNavItem("Settings", Icons.Default.Settings, Screen.Settings),
)
