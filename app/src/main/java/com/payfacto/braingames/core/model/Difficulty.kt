package com.payfacto.braingames.core.model

enum class Difficulty {
    VERY_EASY,
    EASY,
    MEDIUM,
    HARD,
    EXPERT;

    companion object {
        /** Parses a route argument, falling back to [MEDIUM] for a missing or unknown value. */
        fun fromRouteArg(value: String?): Difficulty =
            entries.firstOrNull { it.name == value } ?: MEDIUM
    }
}
