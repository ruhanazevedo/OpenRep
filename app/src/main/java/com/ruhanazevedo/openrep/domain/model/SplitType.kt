package com.ruhanazevedo.openrep.domain.model

enum class SplitType(val label: String, val description: String) {
    A("A", "Full-body every session"),
    AB("AB", "Two alternating sessions (A and B)"),
    ABC("ABC", "Three alternating sessions"),
    PPL("PPL", "Push / Pull / Legs"),
    AA("Antagonist", "Paired antagonist muscle groups per session");

    companion object {
        fun from(value: String): SplitType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: A
    }
}
