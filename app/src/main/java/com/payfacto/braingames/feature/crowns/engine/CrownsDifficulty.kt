package com.payfacto.braingames.feature.crowns.engine

import com.payfacto.braingames.core.model.Difficulty

/** Difficulty = grid size plus the hardest technique the puzzle may (or must) need. */
object CrownsDifficulty {

    fun sizeFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.VERY_EASY -> 7
        Difficulty.EASY      -> 8
        Difficulty.MEDIUM    -> 9
        Difficulty.HARD      -> 10
        Difficulty.EXPERT    -> 11
    }

    /**
     * Easy levels must be solvable with single candidates and confinement only.
     * Hard levels must need a neighbour elimination or a group at least once.
     * Medium takes anything the solver can do.
     */
    fun accepts(difficulty: Difficulty, hardest: Technique?): Boolean {
        val level = hardest ?: Technique.SINGLE_CANDIDATE
        return when (difficulty) {
            Difficulty.VERY_EASY, Difficulty.EASY -> level <= Technique.CONFINED_AREA
            Difficulty.MEDIUM -> true
            Difficulty.HARD, Difficulty.EXPERT -> level >= Technique.NEIGHBOUR_ELIMINATION
        }
    }
}
