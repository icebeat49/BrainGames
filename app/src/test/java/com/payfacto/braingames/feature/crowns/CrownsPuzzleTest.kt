package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.feature.crowns.CrownsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CellMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CrownsPuzzleTest {

    private val puzzle = uniqueFiveByFive
    private fun cell(row: Int, col: Int) = puzzle.cellAt(row, col)

    @Test
    fun `a corner has 3 neighbours and 2 edge neighbours`() {
        assertEquals(setOf(cell(0, 1), cell(1, 0), cell(1, 1)), puzzle.neighboursOf(cell(0, 0)).toSet())
        assertEquals(setOf(cell(0, 1), cell(1, 0)), puzzle.orthogonalNeighboursOf(cell(0, 0)).toSet())
    }

    @Test
    fun `an inner cell has 8 neighbours and 4 edge neighbours`() {
        assertEquals(8, puzzle.neighboursOf(cell(2, 2)).size)
        assertEquals(4, puzzle.orthogonalNeighboursOf(cell(2, 2)).size)
    }

    @Test
    fun `touching includes diagonals but not the cell itself`() {
        assertTrue(puzzle.touches(cell(1, 1), cell(2, 2)))
        assertFalse(puzzle.touches(cell(1, 1), cell(1, 1)))
        assertFalse(puzzle.touches(cell(0, 0), cell(0, 2)))
    }

    @Test
    fun `solution cells follow the solution columns`() {
        assertEquals(
            setOf(cell(0, 0), cell(1, 2), cell(2, 4), cell(3, 1), cell(4, 3)),
            puzzle.solutionCells
        )
    }

    @Test
    fun `the puzzle copies arrays in and out`() {
        val regions = puzzle.regions
        regions[0] = 4

        assertEquals(0, puzzle.regionOf(0))
    }

    @Test
    fun `boards are immutable snapshots`() {
        val empty = CrownsBoard.empty(5)

        val marked = empty.withMark(3, CellMark.CROSS)

        assertEquals(CellMark.EMPTY, empty[3])
        assertEquals(CellMark.CROSS, marked[3])
    }

    @Test
    fun `a change that changes nothing returns the same board`() {
        val empty = CrownsBoard.empty(5)

        assertSame(empty, empty.withMark(3, CellMark.EMPTY))
    }
}
