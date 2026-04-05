package com.ruhanazevedo.openrep

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ruhanazevedo.openrep.navigation.Screen
import com.ruhanazevedo.openrep.navigation.bottomNavItems
import com.ruhanazevedo.openrep.ui.screens.AddExerciseScreen
import com.ruhanazevedo.openrep.ui.screens.ExerciseDetailScreen
import com.ruhanazevedo.openrep.ui.screens.GenerateFilterScreen
import com.ruhanazevedo.openrep.ui.screens.GenerateScreen
import com.ruhanazevedo.openrep.ui.screens.GeneratedPlanScreen
import com.ruhanazevedo.openrep.ui.screens.HistoryScreen
import com.ruhanazevedo.openrep.ui.screens.LibraryScreen
import com.ruhanazevedo.openrep.ui.screens.PlanDetailScreen
import com.ruhanazevedo.openrep.ui.screens.SearchYouTubeScreen
import com.ruhanazevedo.openrep.ui.screens.SessionDetailScreen
import com.ruhanazevedo.openrep.ui.screens.SessionScreen
import com.ruhanazevedo.openrep.ui.screens.SettingsScreen
import com.ruhanazevedo.openrep.ui.viewmodel.GenerationSharedViewModel
import com.ruhanazevedo.openrep.ui.viewmodel.HistoryViewModel

@Composable
fun WorkoutGeneratorNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelRoutes = bottomNavItems.map { it.screen.route }
    val showBottomBar = currentDestination?.route in topLevelRoutes

    // Shared ViewModel scoped to the NavHost — survives GenerateFilter → GeneratedPlan transition
    val sharedViewModel: GenerationSharedViewModel = hiltViewModel()
    // History ViewModel for badge count
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val historyUiState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val savedPlanCount = historyUiState.plans.size

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isHistory = item.screen == Screen.History
                        NavigationBarItem(
                            icon = {
                                if (isHistory && savedPlanCount > 0) {
                                    BadgedBox(badge = { Badge { Text(savedPlanCount.toString()) } }) {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            },
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
            startDestination = Screen.History.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
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
                    onPlanClick = { id ->
                        navController.navigate(Screen.PlanDetail.createRoute(id))
                    },
                    onSessionDetailClick = { id ->
                        navController.navigate(Screen.SessionDetail.createRoute(id))
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
                    onEdit = { id -> navController.navigate(Screen.EditExercise.createRoute(id)) },
                    onSearchYouTube = { id -> navController.navigate(Screen.SearchYouTube.createRoute(id)) }
                )
            }

            composable(Screen.AddExercise.route) {
                AddExerciseScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.EditExercise.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) { backStackEntry ->
                AddExerciseScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.GenerateFilter.route) {
                GenerateFilterScreen(
                    onGenerate = { input ->
                        sharedViewModel.setInput(input)
                        navController.navigate(Screen.GeneratedPlan.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.GeneratedPlan.route) {
                val input by sharedViewModel.input.collectAsStateWithLifecycle()
                val generationTrigger by sharedViewModel.generationTrigger.collectAsStateWithLifecycle()
                GeneratedPlanScreen(
                    input = input,
                    generationTrigger = generationTrigger,
                    onSave = { planId ->
                        // Pop GenerateFilter + GeneratedPlan off the stack, then go to PlanDetail
                        navController.navigate(Screen.PlanDetail.createRoute(planId)) {
                            popUpTo(Screen.Generate.route) { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onExerciseDetail = { id ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(id))
                    }
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
                    onBack = { navController.popBackStack() },
                    onExerciseDetail = { id ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.Session.route,
                arguments = listOf(navArgument("planId") { type = NavType.StringType })
            ) { backStackEntry ->
                SessionScreen(
                    planId = backStackEntry.arguments?.getString("planId") ?: "",
                    onFinish = { navController.popBackStack(Screen.History.route, false) },
                    onBack = { navController.popBackStack() },
                    onExerciseDetail = { exerciseId ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(exerciseId))
                    }
                )
            }

            composable(
                route = Screen.SearchYouTube.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) {
                SearchYouTubeScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.SessionDetail.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) {
                SessionDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
