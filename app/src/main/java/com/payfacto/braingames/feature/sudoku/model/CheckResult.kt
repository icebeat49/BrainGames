package com.payfacto.braingames.feature.sudoku.model

/** Outcome of the player asking to check a Hardcore grid. */
sealed interface CheckResult {
    data object Solved : CheckResult

    /** [wrongCells] are the indices whose value differs from the solution (empty counts as wrong). */
    data class Incorrect(val wrongCells: Set<Int>) : CheckResult
}

/** A check that failed and is waiting for the player's choice in the dialog. */
data class FailedCheck(
    val wrongCells: Set<Int>,
    val isCountShown: Boolean = false
) {
    val wrongCount: Int get() = wrongCells.size
}
