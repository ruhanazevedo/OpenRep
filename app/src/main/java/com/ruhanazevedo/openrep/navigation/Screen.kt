package com.ruhanazevedo.openrep.navigation

sealed class Screen(val route: String) {
    // Bottom nav destinations
    data object Library : Screen("library")
    data object Generate : Screen("generate")
    data object History : Screen("history")
    data object Settings : Screen("settings")

    // Nested destinations
    data object ExerciseDetail : Screen("exercise_detail/{exerciseId}") {
        fun createRoute(exerciseId: String) = "exercise_detail/$exerciseId"
    }
    data object AddExercise : Screen("add_exercise")
    data object EditExercise : Screen("edit_exercise/{exerciseId}") {
        fun createRoute(exerciseId: String) = "edit_exercise/$exerciseId"
    }
    data object GenerateFilter : Screen("generate_filter")
    data object GeneratedPlan : Screen("generated_plan")
    data object PlanDetail : Screen("plan_detail/{planId}") {
        fun createRoute(planId: String) = "plan_detail/$planId"
    }
    data object Session : Screen("session/{planId}") {
        fun createRoute(planId: String) = "session/$planId"
    }
    data object SearchYouTube : Screen("search_youtube/{exerciseId}") {
        fun createRoute(exerciseId: String) = "search_youtube/$exerciseId"
    }
    data object SessionDetail : Screen("session_detail/{sessionId}") {
        fun createRoute(sessionId: String) = "session_detail/$sessionId"
    }
}
