package com.ruhanazevedo.workoutgenerator

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ruhanazevedo.workoutgenerator.navigation.Screen
import com.ruhanazevedo.workoutgenerator.navigation.bottomNavItems
import com.ruhanazevedo.workoutgenerator.ui.screens.AddExerciseScreen
import com.ruhanazevedo.workoutgenerator.ui.screens.ExerciseDetailScreen
import com.ruhanazevedo.workoutgenerator.ui.screens.GenerateFilterScreen
import com.ruhanazevedo.workoutgenerator.ui.screens.GenerateScreen
import com.ruhanazevedo.workoutgenerator.ui.screens.GeneratedPlanScreen
import com.ruhanazevedo.workoutgenerator.ui.screens.HistoryScreen
import com.ruhanazevedo.workoutgenerator.ui.screens.LibraryScreen
import com.ruhanazevedo.workoutgenerator.ui.screens.PlanDetailScreen
import com.ruhanazevedo.workoutgenerator.ui.screens.SessionScreen
import com.ruhanazevedo.workoutgenerator.ui.screens.SettingsScreen

@Composable
fun WorkoutGeneratorNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelRoutes = bottomNavItems.map { it.screen.route }
    val showBottomBar = currentDestination?.route in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Bottom nav top-level destinations
            composable(Screen.Library.route) {
                LibraryScreen(
                    onExerciseClick = { id ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(id))
                    },
                    onAddExerciseClick = {
                        navController.navigate(Screen.AddExercise.route)
                    }
                )
            }
            composable(Screen.Generate.route) {
                GenerateScreen(
                    onStartGenerate = {
                        navController.navigate(Screen.GenerateFilter.route)
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onSessionClick = { id ->
                        navController.navigate(Screen.PlanDetail.createRoute(id))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            // Deep destinations
            composable(
                route = Screen.ExerciseDetail.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) { backStackEntry ->
                ExerciseDetailScreen(
                    exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: "",
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Screen.EditExercise.createRoute(id)) }
                )
            }

            composable(Screen.AddExercise.route) {
                AddExerciseScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.EditExercise.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) {
                AddExerciseScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.GenerateFilter.route) {
                GenerateFilterScreen(
                    onGenerate = { navController.navigate(Screen.GeneratedPlan.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.GeneratedPlan.route) {
                GeneratedPlanScreen(
                    onSave = { navController.popBackStack(Screen.History.route, false) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PlanDetail.route,
                arguments = listOf(navArgument("planId") { type = NavType.StringType })
            ) { backStackEntry ->
                val planId = backStackEntry.arguments?.getString("planId") ?: ""
                PlanDetailScreen(
                    planId = planId,
                    onStartWorkout = { navController.navigate(Screen.Session.createRoute(planId)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Session.route,
                arguments = listOf(navArgument("planId") { type = NavType.StringType })
            ) { backStackEntry ->
                SessionScreen(
                    planId = backStackEntry.arguments?.getString("planId") ?: "",
                    onFinish = { navController.popBackStack(Screen.History.route, false) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
