package com.payfacto.braingames.feature.crowns.input

import com.payfacto.braingames.core.ui.grid.GridGeometry
import com.payfacto.braingames.core.ui.grid.GridPoint
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CellMark
import kotlin.math.hypot

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
 * A completed tap, in grid units.
 *
 * @param downTimeMillis / [upTimeMillis] the pointer's down and up uptimes.
 * @param doubleTapTimeoutMillis the system double-tap timeout.
 * @param snapRadiusCells how far (in cells) a second tap may land from the first and still
 *   count as a double tap on the first cell: the touch slop divided by the cell size.
 */
data class TapEvent(
    val point: GridPoint,
    val downTimeMillis: Long,
    val upTimeMillis: Long,
    val doubleTapTimeoutMillis: Long,
    val snapRadiusCells: Float
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
 * - Drag: the start cell decides the mode for the whole gesture, applied to every cell the
 *   finger crosses (interpolated between samples), as one undo step:
 *   - starts on EMPTY → paint: EMPTY cells get X (or ?), other cells are left alone;
 *   - starts on X or ? → erase: X and ? cells are cleared, whatever the toggle says;
 *   - starts on a crown → nothing at all.
 *   A drag never places, removes or changes a crown.
 */
class CrownsGestures {

    private enum class DragMode { PAINT, ERASE, NONE }

    private data class LastTap(
        val cell: Int,
        val point: GridPoint,
        val upTimeMillis: Long,
        val markBefore: CellMark
    )

    private var lastTap: LastTap? = null
    private var dragPoint: GridPoint? = null
    private var dragMode = DragMode.NONE
    private var paintMark = CellMark.CROSS
    private var dragHasEdited = false

    /**
     * A completed tap. The second tap of a double tap counts on the first tap's cell when it
     * lands within [TapEvent.snapRadiusCells] of the first tap, even across a cell border:
     * with ~30dp cells on a small phone, finger jitter would otherwise turn a double tap into
     * two crosses on neighbouring cells. Returns null for a tap outside the grid.
     */
    fun tap(event: TapEvent, board: CrownsBoard, questionMode: Boolean): BoardEdit? {
        val point = event.point
        val tappedCell = GridGeometry.cellAt(point, board.size, board.size)
        val previous = lastTap
        if (previous != null &&
            event.downTimeMillis - previous.upTimeMillis <= event.doubleTapTimeoutMillis &&
            (tappedCell == previous.cell || distance(point, previous.point) <= event.snapRadiusCells)
        ) {
            lastTap = null // a third tap starts over as a single tap
            val result = if (previous.markBefore == CellMark.CROWN) CellMark.EMPTY else CellMark.CROWN
            return BoardEdit(mapOf(previous.cell to result), continuesPrevious = true)
        }

        val cell = tappedCell ?: return null
        val before = board[cell]
        val after = if (before == CellMark.EMPTY) markFor(questionMode) else CellMark.EMPTY
        lastTap = LastTap(cell, point, event.upTimeMillis, before)
        return BoardEdit(mapOf(cell to after), continuesPrevious = false)
    }

    private fun distance(a: GridPoint, b: GridPoint): Float = hypot(a.row - b.row, a.column - b.column)

    /** The finger moved past touch slop from [start]: the start cell sets the drag mode, then is edited. */
    fun dragStart(start: GridPoint, board: CrownsBoard, questionMode: Boolean): BoardEdit? {
        lastTap = null
        dragPoint = start
        dragHasEdited = false
        paintMark = markFor(questionMode)
        val startCell = GridGeometry.cellAt(start, board.size, board.size)
        dragMode = when (startCell?.let { board[it] }) {
            CellMark.EMPTY -> DragMode.PAINT
            CellMark.CROSS, CellMark.QUESTION -> DragMode.ERASE
            CellMark.CROWN, null -> DragMode.NONE
        }
        return edit(GridGeometry.cellsAlong(start, start, board.size, board.size), board)
    }

    /** Edits every cell between the previous sample and [to]. */
    fun dragMove(to: GridPoint, board: CrownsBoard): BoardEdit? {
        val from = dragPoint ?: return null
        dragPoint = to
        return edit(GridGeometry.cellsAlong(from, to, board.size, board.size), board)
    }

    fun dragEnd() {
        dragPoint = null
        dragMode = DragMode.NONE
        dragHasEdited = false
    }

    /** Applies the drag mode to [cells]; crowns are never touched. */
    private fun edit(cells: List<Int>, board: CrownsBoard): BoardEdit? {
        val changes: Map<Int, CellMark> = when (dragMode) {
            DragMode.PAINT -> cells.filter { board[it] == CellMark.EMPTY }.associateWith { paintMark }
            DragMode.ERASE -> cells.filter { board[it] == CellMark.CROSS || board[it] == CellMark.QUESTION }
                .associateWith { CellMark.EMPTY }
            DragMode.NONE -> emptyMap()
        }
        if (changes.isEmpty()) return null
        return BoardEdit(changes, continuesPrevious = dragHasEdited).also { dragHasEdited = true }
    }

    private fun markFor(questionMode: Boolean) = if (questionMode) CellMark.QUESTION else CellMark.CROSS
}
