package com.payfacto.braingames.core.model

enum class Difficulty {
    VERY_EASY,
    EASY,
    MEDIUM,
    HARD,
    EXPERT,

    /** Above Expert, only in games that list it in [GameType.levels] (Crowns 12×12). */
    MASTER;

    companion object {
        /** The five levels every game offers. */
        val STANDARD: List<Difficulty> = listOf(VERY_EASY, EASY, MEDIUM, HARD, EXPERT)

        /** Parses a route argument, falling back to [MEDIUM] for a missing or unknown value. */
        fun fromRouteArg(value: String?): Difficulty =
            entries.firstOrNull { it.name == value } ?: MEDIUM
    }
}
