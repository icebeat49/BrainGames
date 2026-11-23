package com.payfacto.braingames.feature.animals

import com.payfacto.braingames.core.ui.grid.GridPoint
import com.payfacto.braingames.core.undo.UndoStack
import com.payfacto.braingames.feature.animals.input.AnimalsGestures
import com.payfacto.braingames.feature.animals.input.BoardEdit
import com.payfacto.braingames.feature.animals.model.AnimalsBoard
import com.payfacto.braingames.feature.animals.model.CellMark
import com.payfacto.braingames.feature.animals.model.CellMark.ANIMAL
import com.payfacto.braingames.feature.animals.model.CellMark.CROSS
import com.payfacto.braingames.feature.animals.model.CellMark.EMPTY
import com.payfacto.braingames.feature.animals.model.CellMark.QUESTION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnimalsGesturesTest {

    /**
     * Applies edits like the ViewModel does: a new undo entry unless the edit continues
     * the previous action.
     */
    private inner class Harness(size: Int = 6) {
        val gestures = AnimalsGestures()
        val undo = UndoStack<AnimalsBoard>()
        var board = AnimalsBoard.empty(size)
        var questionMode = false
        private var clock = 1_000L

        fun apply(edit: BoardEdit?) {
            if (edit == null) return
            if (!edit.continuesPrevious) undo.push(board)
            edit.changes.forEach { (cell, mark) -> board = board.withMark(cell, mark) }
        }

        /** A tap [gapMillis] after the previous one, lasting 50 ms. */
        fun tap(cell: Int, gapMillis: Long = 1_000) {
            val down = clock + gapMillis
            val up = down + 50
            clock = up
            apply(gestures.tap(cell, down, up, DOUBLE_TAP_TIMEOUT, board, questionMode))
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
    fun `tap on a cross, a question mark or an animal clears it`() {
        val h = Harness()
        h.set(1, CROSS)
        h.set(2, QUESTION)
        h.set(3, ANIMAL)

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
    fun `double tap on empty places an animal`() {
        val h = Harness()

        h.doubleTap(7)

        assertEquals(ANIMAL, h.board[7])
    }

    @Test
    fun `the cross placed by the first tap and the animal are one undo step`() {
        val h = Harness()

        h.doubleTap(7)
        h.undoOnce()

        assertEquals(EMPTY, h.board[7])
        assertFalse(h.undo.canUndo)
    }

    @Test
    fun `double tap on a cross or a question mark places an animal`() {
        val h = Harness()
        h.set(1, CROSS)
        h.set(2, QUESTION)

        h.doubleTap(1)
        h.doubleTap(2)

        assertEquals(listOf(ANIMAL, ANIMAL), listOf(h.board[1], h.board[2]))
    }

    @Test
    fun `double tap on an animal clears it`() {
        val h = Harness()
        h.set(7, ANIMAL)

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
    fun `double tap with the question toggle on still places an animal`() {
        val h = Harness().apply { questionMode = true }

        h.doubleTap(7)

        assertEquals(ANIMAL, h.board[7])
    }

    // ── Drag ────────────────────────────────────────────────────────────────

    @Test
    fun `a drag crosses every empty cell it passes`() {
        val h = Harness()

        h.drag(middleOf(0, 0), middleOf(0, 5))

        assertEquals((0..5).map { CROSS }, (0..5).map { h.board[it] })
    }

    @Test
    fun `a drag never overwrites animals or existing marks`() {
        val h = Harness()
        h.set(1, ANIMAL)
        h.set(2, QUESTION)
        h.set(3, CROSS)

        h.drag(middleOf(0, 0), middleOf(0, 5))

        assertEquals(listOf(CROSS, ANIMAL, QUESTION, CROSS, CROSS, CROSS), (0..5).map { h.board[it] })
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

        assertEquals(AnimalsBoard.empty(6), h.board)
    }

    @Test
    fun `a drag that starts on marked cells is still one undo step`() {
        val h = Harness()
        h.set(0, ANIMAL)

        h.drag(middleOf(0, 0), middleOf(0, 1), middleOf(0, 3))

        assertEquals(1, h.undo.size)
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
    }
}
