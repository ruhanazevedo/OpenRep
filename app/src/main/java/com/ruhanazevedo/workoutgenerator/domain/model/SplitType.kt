package com.ruhanazevedo.workoutgenerator.domain.model

enum class SplitType(val label: String, val description: String) {
    A("A", "Full-body every session"),
    AB("AB", "Two alternating sessions (A and B)"),
    ABC("ABC", "Three alternating sessions"),
    PPL("PPL", "Push / Pull / Legs"),
    AA("AA", "Same workout every day");

    companion object {
        fun from(value: String): SplitType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: A
    }
}
