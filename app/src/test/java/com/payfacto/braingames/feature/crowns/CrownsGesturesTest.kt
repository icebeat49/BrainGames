package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.core.ui.grid.GridPoint
import com.payfacto.braingames.core.undo.UndoStack
import com.payfacto.braingames.feature.crowns.input.CrownsGestures
import com.payfacto.braingames.feature.crowns.input.BoardEdit
import com.payfacto.braingames.feature.crowns.input.TapEvent
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CellMark
import com.payfacto.braingames.feature.crowns.model.CellMark.CROWN
import com.payfacto.braingames.feature.crowns.model.CellMark.CROSS
import com.payfacto.braingames.feature.crowns.model.CellMark.EMPTY
import com.payfacto.braingames.feature.crowns.model.CellMark.QUESTION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CrownsGesturesTest {

    /**
     * Applies edits like the ViewModel does: a new undo entry unless the edit continues
     * the previous action.
     */
    private inner class Harness(size: Int = 6) {
        val gestures = CrownsGestures()
        val undo = UndoStack<CrownsBoard>()
        var board = CrownsBoard.empty(size)
        var questionMode = false
        private var clock = 1_000L

        fun apply(edit: BoardEdit?) {
            if (edit == null) return
            if (!edit.continuesPrevious) undo.push(board)
            edit.changes.forEach { (cell, mark) -> board = board.withMark(cell, mark) }
        }

        /** A tap in the middle of [cell], [gapMillis] after the previous one, lasting 50 ms. */
        fun tap(cell: Int, gapMillis: Long = 1_000) =
            tapAt(GridPoint(cell / board.size + 0.5f, cell % board.size + 0.5f), gapMillis)

        /** A tap at an exact position, in grid units. */
        fun tapAt(point: GridPoint, gapMillis: Long = 1_000) {
            val down = clock + gapMillis
            val up = down + 50
            clock = up
            apply(gestures.tap(TapEvent(point, down, up, DOUBLE_TAP_TIMEOUT, SNAP_RADIUS_CELLS), board, questionMode))
        }

        fun doubleTap(cell: Int) {
            tap(cell)
            tap(cell, gapMillis = 100)
        }

        fun drag(vararg points: GridPoint) {
            apply(gestures.dragStart(points.first(), board, questionMode))
            points.drop(1).forEach { apply(gestures.dragMove(it, board)) }
            gestures.dragEnd()
        }

        fun set(cell: Int, mark: CellMark) {
            board = board.withMark(cell, mark)
        }

        fun undoOnce() {
            undo.pop()?.let { board = it }
        }
    }

    private fun middleOf(row: Int, col: Int) = GridPoint(row + 0.5f, col + 0.5f)

    // ── Single tap ──────────────────────────────────────────────────────────

    @Test
    fun `tap on empty places a cross at once`() {
        val h = Harness()

        h.tap(7)

        assertEquals(CROSS, h.board[7])
    }

    @Test
    fun `tap on empty places a question mark when the toggle is on`() {
        val h = Harness().apply { questionMode = true }

        h.tap(7)

        assertEquals(QUESTION, h.board[7])
    }

    @Test
    fun `tap on a cross, a question mark or a crown clears it`() {
        val h = Harness()
        h.set(1, CROSS)
        h.set(2, QUESTION)
        h.set(3, CROWN)

        h.tap(1)
        h.tap(2)
        h.tap(3)

        assertEquals(listOf(EMPTY, EMPTY, EMPTY), listOf(h.board[1], h.board[2], h.board[3]))
    }

    @Test
    fun `two slow taps are two single taps`() {
        val h = Harness()

        h.tap(7)
        h.tap(7, gapMillis = DOUBLE_TAP_TIMEOUT + 1)

        assertEquals(EMPTY, h.board[7])
        assertEquals(2, h.undo.size)
    }

    @Test
    fun `quick taps on two different cells are two single taps`() {
        val h = Harness()

        h.tap(7)
        h.tap(8, gapMillis = 100)

        assertEquals(listOf(CROSS, CROSS), listOf(h.board[7], h.board[8]))
    }

    // ── Double tap ──────────────────────────────────────────────────────────

    @Test
    fun `double tap on empty places a crown`() {
        val h = Harness()

        h.doubleTap(7)

        assertEquals(CROWN, h.board[7])
    }

    @Test
    fun `the cross placed by the first tap and the crown are one undo step`() {
        val h = Harness()

        h.doubleTap(7)
        h.undoOnce()

        assertEquals(EMPTY, h.board[7])
        assertFalse(h.undo.canUndo)
    }

    @Test
    fun `double tap on a cross or a question mark places a crown`() {
        val h = Harness()
        h.set(1, CROSS)
        h.set(2, QUESTION)

        h.doubleTap(1)
        h.doubleTap(2)

        assertEquals(listOf(CROWN, CROWN), listOf(h.board[1], h.board[2]))
    }

    @Test
    fun `double tap on a crown clears it`() {
        val h = Harness()
        h.set(7, CROWN)

        h.doubleTap(7)

        assertEquals(EMPTY, h.board[7])
    }

    @Test
    fun `double tap on a cross restores the cross in one undo`() {
        val h = Harness()
        h.set(7, CROSS)

        h.doubleTap(7)
        h.undoOnce()

        assertEquals(CROSS, h.board[7])
    }

    @Test
    fun `a third quick tap starts over as a single tap`() {
        val h = Harness()

        h.doubleTap(7)
        h.tap(7, gapMillis = 100)

        assertEquals(EMPTY, h.board[7])
    }

    @Test
    fun `double tap with the question toggle on still places a crown`() {
        val h = Harness().apply { questionMode = true }

        h.doubleTap(7)

        assertEquals(CROWN, h.board[7])
    }

    // ── Double tap on small cells ───────────────────────────────────────────

    @Test
    fun `a second tap just across the border still makes a crown on the first cell`() {
        val h = Harness()

        h.tapAt(GridPoint(1.5f, 1.95f))                 // cell (1,1), near its right edge
        h.tapAt(GridPoint(1.5f, 2.05f), gapMillis = 100) // cell (1,2), 0.1 cell away

        assertEquals(CROWN, h.board[7])
        assertEquals(EMPTY, h.board[8])
        assertEquals(1, h.undo.size)
    }

    @Test
    fun `a quick second tap further than the slop on a neighbour is two single taps`() {
        val h = Harness()

        h.tapAt(GridPoint(1.5f, 1.5f))
        h.tapAt(GridPoint(1.5f, 2.5f), gapMillis = 100) // one whole cell away

        assertEquals(listOf(CROSS, CROSS), listOf(h.board[7], h.board[8]))
    }

    @Test
    fun `a tap outside the grid does nothing`() {
        val h = Harness()

        h.tapAt(GridPoint(-0.5f, 2.5f))

        assertEquals(CrownsBoard.empty(6), h.board)
    }

    // ── Drag ────────────────────────────────────────────────────────────────

    @Test
    fun `a drag crosses every empty cell it passes`() {
        val h = Harness()

        h.drag(middleOf(0, 0), middleOf(0, 5))

        assertEquals((0..5).map { CROSS }, (0..5).map { h.board[it] })
    }

    @Test
    fun `a drag never overwrites crowns or existing marks`() {
        val h = Harness()
        h.set(1, CROWN)
        h.set(2, QUESTION)
        h.set(3, CROSS)

        h.drag(middleOf(0, 0), middleOf(0, 5))

        assertEquals(listOf(CROSS, CROWN, QUESTION, CROSS, CROSS, CROSS), (0..5).map { h.board[it] })
    }

    @Test
    fun `a fast swipe with only two samples skips no cell`() {
        val h = Harness()

        h.drag(GridPoint(3.5f, 0.1f), GridPoint(3.5f, 5.9f))

        assertEquals((0..5).map { CROSS }, (0..5).map { h.board[3 * 6 + it] })
    }

    @Test
    fun `a drag paints question marks when the toggle is on`() {
        val h = Harness().apply { questionMode = true }

        h.drag(middleOf(1, 0), middleOf(1, 2))

        assertEquals(listOf(QUESTION, QUESTION, QUESTION), (6..8).map { h.board[it] })
    }

    @Test
    fun `a whole drag is one undo step`() {
        val h = Harness()

        h.drag(middleOf(0, 0), middleOf(0, 2), middleOf(2, 2), middleOf(2, 5))
        h.undoOnce()

        assertEquals(CrownsBoard.empty(6), h.board)
    }

    @Test
    fun `a paint drag that starts on an empty cell next to marks is one undo step`() {
        val h = Harness()
        h.set(1, CROSS)

        h.drag(middleOf(0, 0), middleOf(0, 1), middleOf(0, 3))

        assertEquals(listOf(CROSS, CROSS, CROSS, CROSS), (0..3).map { h.board[it] })
        assertEquals(1, h.undo.size)
    }

    // ── Erase drag ──────────────────────────────────────────────────────────

    @Test
    fun `a drag starting on a cross erases crosses and question marks in its path`() {
        val h = Harness()
        h.set(0, CROSS)
        h.set(1, QUESTION)
        h.set(3, CROSS)
        h.set(5, QUESTION)

        h.drag(middleOf(0, 0), middleOf(0, 5))

        assertEquals((0..5).map { EMPTY }, (0..5).map { h.board[it] })
    }

    @Test
    fun `an erase drag leaves crowns and empty cells untouched`() {
        val h = Harness()
        h.set(0, CROSS)
        h.set(2, CROWN)
        h.set(4, CROSS)

        h.drag(middleOf(0, 0), middleOf(0, 5))

        assertEquals(listOf(EMPTY, EMPTY, CROWN, EMPTY, EMPTY, EMPTY), (0..5).map { h.board[it] })
    }

    @Test
    fun `a drag starting on a question mark erases too`() {
        val h = Harness()
        h.set(6, QUESTION)
        h.set(7, CROSS)

        h.drag(middleOf(1, 0), middleOf(1, 1))

        assertEquals(listOf(EMPTY, EMPTY), listOf(h.board[6], h.board[7]))
    }

    @Test
    fun `an erase drag ignores the question toggle`() {
        val h = Harness().apply { questionMode = true }
        h.set(0, CROSS)
        h.set(1, CROSS)

        h.drag(middleOf(0, 0), middleOf(0, 2))

        assertEquals(listOf(EMPTY, EMPTY, EMPTY), (0..2).map { h.board[it] })
    }

    @Test
    fun `a fast erase swipe with only two samples skips no cell`() {
        val h = Harness()
        (0..5).forEach { h.set(4 * 6 + it, CROSS) }

        h.drag(GridPoint(4.5f, 0.1f), GridPoint(4.5f, 5.9f))

        assertEquals((0..5).map { EMPTY }, (0..5).map { h.board[4 * 6 + it] })
    }

    @Test
    fun `a whole erase drag is one undo step`() {
        val h = Harness()
        (0..5).forEach { h.set(it, CROSS) }
        val before = h.board

        h.drag(middleOf(0, 0), middleOf(0, 2), middleOf(0, 5))
        h.undoOnce()

        assertEquals(before, h.board)
    }

    @Test
    fun `a drag starting on a crown does nothing and records no undo`() {
        val h = Harness()
        h.set(0, CROWN)
        h.set(2, CROSS)
        val before = h.board

        h.drag(middleOf(0, 0), middleOf(0, 5))

        assertEquals(before, h.board)
        assertFalse(h.undo.canUndo)
    }

    @Test
    fun `a drag between the two taps cancels the double tap`() {
        val h = Harness()

        h.tap(7)
        h.drag(middleOf(3, 0), middleOf(3, 2))
        h.tap(7, gapMillis = 100)

        assertEquals(EMPTY, h.board[7])
    }

    private companion object {
        const val DOUBLE_TAP_TIMEOUT = 300L
        /** About an 8dp touch slop on a 30dp cell (11×11 on a small phone). */
        const val SNAP_RADIUS_CELLS = 0.27f
    }
}
