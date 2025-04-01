package com.ruhanazevedo.workoutgenerator.domain.model

object Equipment {
    const val BARBELL = "Barbell"
    const val DUMBBELL = "Dumbbell"
    const val CABLE = "Cable"
    const val MACHINE = "Machine"
    const val BODYWEIGHT = "Bodyweight"
    const val KETTLEBELL = "Kettlebell"
    const val RESISTANCE_BAND = "Resistance Band"
    const val OTHER = "Other"

    val ALL = listOf(BARBELL, DUMBBELL, CABLE, MACHINE, BODYWEIGHT, KETTLEBELL, RESISTANCE_BAND, OTHER)
}
