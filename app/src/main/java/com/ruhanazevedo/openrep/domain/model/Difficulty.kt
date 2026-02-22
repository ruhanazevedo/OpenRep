package com.ruhanazevedo.openrep.domain.model

enum class Difficulty {
    Beginner,
    Intermediate,
    Advanced;

    companion object {
        fun from(value: String): Difficulty = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: Beginner
    }
}
