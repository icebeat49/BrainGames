package com.payfacto.braingames.core.model

/**
 * How much help a game gives. Each game decides what the modes mean for it
 * (e.g. SudokuAssists); the game catalog says which games offer a choice.
 */
enum class GameMode {
    /** Live mistake marking, hints, automatic win. */
    CLASSIC,
    /** Pen and paper: no help, the player checks the grid when done. */
    HARDCORE;

    companion object {
        /** Parses a route argument or stored value, falling back to [CLASSIC]. */
        fun fromName(value: String?): GameMode = entries.firstOrNull { it.name == value } ?: CLASSIC
    }
}
