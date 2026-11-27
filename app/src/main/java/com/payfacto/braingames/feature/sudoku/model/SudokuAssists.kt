package com.payfacto.braingames.feature.sudoku.model

import com.payfacto.braingames.core.model.GameMode

/**
 * Which help Sudoku gives in each [GameMode]. Every assist is read in one place: the grid
 * for highlights and conflict marks, the ViewModel for hints and win detection.
 */
data class SudokuAssists(
    val highlightPeers: Boolean,
    val highlightSameNumber: Boolean,
    val showConflicts: Boolean,
    /** A digit that isn't the solution's is shown in red as soon as it is written. */
    val showWrongDigits: Boolean,
    /** 0 hides the hint button and makes useHint() a no-op. */
    val maxHints: Int,
    /** false: the player taps Check when the grid is full. */
    val autoDetectWin: Boolean,
    /** Writing a digit hides that cell's own notes. */
    val clearCellNotesOnEntry: Boolean,
    /** Writing a digit also removes it from the notes of every cell in its row, column and box. */
    val autoRemovePeerNotes: Boolean,
    /** Notes holding the selected cell's digit are highlighted. */
    val highlightMatchingNotes: Boolean,
) {
    val hintsAllowed: Boolean get() = maxHints > 0

    companion object {
        val Classic = SudokuAssists(
            highlightPeers = true,
            highlightSameNumber = true,
            showConflicts = true,
            showWrongDigits = true,
            maxHints = 5,
            autoDetectWin = true,
            clearCellNotesOnEntry = true,
            autoRemovePeerNotes = true,
            highlightMatchingNotes = true,
        )

        /** Pen and paper: only the selected cell is highlighted. */
        val Hardcore = SudokuAssists(
            highlightPeers = false,
            highlightSameNumber = false,
            showConflicts = false,
            showWrongDigits = false,
            maxHints = 0,
            autoDetectWin = false,
            clearCellNotesOnEntry = true,
            autoRemovePeerNotes = false,
            highlightMatchingNotes = false,
        )

        fun forMode(mode: GameMode): SudokuAssists = when (mode) {
            GameMode.CLASSIC  -> Classic
            GameMode.HARDCORE -> Hardcore
        }
    }
}
