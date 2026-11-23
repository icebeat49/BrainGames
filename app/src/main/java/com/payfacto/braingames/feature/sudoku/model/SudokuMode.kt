package com.payfacto.braingames.feature.sudoku.model

enum class SudokuMode {
    CLASSIC,
    HARDCORE;

    val assists: SudokuAssists
        get() = when (this) {
            CLASSIC  -> SudokuAssists.Classic
            HARDCORE -> SudokuAssists.Hardcore
        }

    companion object {
        /** Parses a route argument or stored value, falling back to [CLASSIC]. */
        fun fromName(value: String?): SudokuMode = entries.firstOrNull { it.name == value } ?: CLASSIC
    }
}

/**
 * Which help the game gives. Every assist is read in one place: the grid for highlights
 * and conflict marks, the ViewModel for hints and win detection.
 */
data class SudokuAssists(
    val highlightPeers: Boolean,
    val highlightSameNumber: Boolean,
    val showConflicts: Boolean,
    /** 0 hides the hint button and makes useHint() a no-op. */
    val maxHints: Int,
    /** false: the player taps Check when the grid is full. */
    val autoDetectWin: Boolean,
    /** Writing a digit hides that cell's own notes. Other cells' notes are never touched. */
    val clearCellNotesOnEntry: Boolean,
) {
    val hintsAllowed: Boolean get() = maxHints > 0

    companion object {
        val Classic = SudokuAssists(
            highlightPeers = true,
            highlightSameNumber = true,
            showConflicts = true,
            maxHints = 5,
            autoDetectWin = true,
            clearCellNotesOnEntry = true,
        )

        /** Pen and paper: only the selected cell is highlighted. */
        val Hardcore = SudokuAssists(
            highlightPeers = false,
            highlightSameNumber = false,
            showConflicts = false,
            maxHints = 0,
            autoDetectWin = false,
            clearCellNotesOnEntry = true,
        )
    }
}
