package com.payfacto.braingames.feature.sudoku.model

/**
 * A cell as it was before a change: what undo puts back. Every Sudoku move (digit,
 * note, erase, hint) touches exactly one cell, so this is a complete snapshot of it.
 */
data class SudokuMove(
    val index: Int,
    val value: Int,
    val notes: Set<Int>
)
