package com.payfacto.braingames.feature.crowns.input

import com.payfacto.braingames.core.ui.grid.GridGeometry
import com.payfacto.braingames.core.ui.grid.GridPoint
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CellMark

/**
 * Marks to write after a gesture.
 *
 * @param continuesPrevious true when this edit belongs to the same player action as the
 *   previous one (the second tap of a double tap, later cells of a drag): the caller must
 *   not record a new undo entry, so one undo reverts the whole action.
 */
data class BoardEdit(
    val changes: Map<Int, CellMark>,
    val continuesPrevious: Boolean
)

/**
 * Turns raw taps and drags into board edits. Pure and stateful (it remembers the last tap
 * for double taps and the current drag), so it is unit-tested without Compose.
 *
 * - Tap on EMPTY → X (or ? when [questionMode] is on), applied at once, no waiting.
 * - Tap on X, ? or CROWN → EMPTY.
 * - Second tap on the same cell within the double-tap timeout → CROWN, replacing what
 *   the first tap did, as the same undo step. If the cell held a crown before the
 *   first tap, the double tap clears it instead.
 * - Drag → paints X (or ?) on every EMPTY cell the finger crosses, interpolated between
 *   samples. Never overwrites a crown or another mark. One undo step per drag.
 */
class CrownsGestures {

    private data class LastTap(val cell: Int, val upTimeMillis: Long, val markBefore: CellMark)

    private var lastTap: LastTap? = null
    private var dragPoint: GridPoint? = null
    private var dragMark = CellMark.CROSS
    private var dragHasEdited = false

    /** A completed tap. Times are the pointer's down and up uptimes in milliseconds. */
    fun tap(
        cell: Int,
        downTimeMillis: Long,
        upTimeMillis: Long,
        doubleTapTimeoutMillis: Long,
        board: CrownsBoard,
        questionMode: Boolean
    ): BoardEdit {
        val previous = lastTap
        if (previous != null && previous.cell == cell &&
            downTimeMillis - previous.upTimeMillis <= doubleTapTimeoutMillis
        ) {
            lastTap = null // a third tap starts over as a single tap
            val result = if (previous.markBefore == CellMark.CROWN) CellMark.EMPTY else CellMark.CROWN
            return BoardEdit(mapOf(cell to result), continuesPrevious = true)
        }

        val before = board[cell]
        val after = if (before == CellMark.EMPTY) paintMark(questionMode) else CellMark.EMPTY
        lastTap = LastTap(cell, upTimeMillis, before)
        return BoardEdit(mapOf(cell to after), continuesPrevious = false)
    }

    /** The finger moved past touch slop from [start]: paint the cell it started on. */
    fun dragStart(start: GridPoint, board: CrownsBoard, questionMode: Boolean): BoardEdit? {
        lastTap = null
        dragPoint = start
        dragMark = paintMark(questionMode)
        dragHasEdited = false
        return paint(GridGeometry.cellsAlong(start, start, board.size, board.size), board)
    }

    /** Paints every cell between the previous sample and [to]. */
    fun dragMove(to: GridPoint, board: CrownsBoard): BoardEdit? {
        val from = dragPoint ?: return null
        dragPoint = to
        return paint(GridGeometry.cellsAlong(from, to, board.size, board.size), board)
    }

    fun dragEnd() {
        dragPoint = null
        dragHasEdited = false
    }

    private fun paint(cells: List<Int>, board: CrownsBoard): BoardEdit? {
        val changes = cells.filter { board[it] == CellMark.EMPTY }.associateWith { dragMark }
        if (changes.isEmpty()) return null
        return BoardEdit(changes, continuesPrevious = dragHasEdited).also { dragHasEdited = true }
    }

    private fun paintMark(questionMode: Boolean) = if (questionMode) CellMark.QUESTION else CellMark.CROSS
}
