package com.payfacto.braingames.feature.sudoku.model

/**
 * A cell as it was before a change: what undo puts back. Most moves touch one cell; placing a
 * digit in Classic also removes it from the notes of the cells it sees, and those snapshots are
 * pushed after the cell's own with [linkedToPrevious] set, so one undo restores them together.
 */
data class SudokuMove(
    val index: Int,
    val value: Int,
    val notes: Set<Int>,
    val linkedToPrevious: Boolean = false
)
