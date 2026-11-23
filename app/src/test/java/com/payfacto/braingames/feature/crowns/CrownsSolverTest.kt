package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.feature.crowns.CrownsTestGrids.parse
import com.payfacto.braingames.feature.crowns.CrownsTestGrids.rowsAsRegions
import com.payfacto.braingames.feature.crowns.CrownsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.crowns.engine.CrownsSolver
import com.payfacto.braingames.feature.crowns.engine.Area
import com.payfacto.braingames.feature.crowns.engine.AreaType
import com.payfacto.braingames.feature.crowns.engine.CandidateGrid
import com.payfacto.braingames.feature.crowns.engine.SolverStep
import com.payfacto.braingames.feature.crowns.engine.Technique
import com.payfacto.braingames.feature.crowns.engine.Techniques
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CellMark
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrownsSolverTest {

    private val puzzle = uniqueFiveByFive
    private fun cell(row: Int, col: Int) = puzzle.cellAt(row, col)

    // ── Full solve ──────────────────────────────────────────────────────────

    @Test
    fun `the hand-made grid is solved by logic alone`() {
        val result = CrownsSolver.solve(puzzle)

        assertTrue(result.solved)
        assertEquals(puzzle.solutionCells, result.steps.flatMap { it.placements }.toSet())
    }

    @Test
    fun `the hand-made grid needs a neighbour elimination`() {
        assertEquals(Technique.NEIGHBOUR_ELIMINATION, CrownsSolver.solve(puzzle).hardest)
    }

    @Test
    fun `no step ever crosses out a solution cell or places a wrong crown`() {
        val steps = CrownsSolver.solve(puzzle).steps

        assertTrue(steps.all { puzzle.solutionCells.containsAll(it.placements) })
        assertTrue(steps.none { step -> step.eliminations.any { it in puzzle.solutionCells } })
    }

    @Test
    fun `a puzzle with several solutions is not solved by logic`() {
        assertFalse(CrownsSolver.solve(rowsAsRegions).solved)
    }

    // ── Individual techniques ───────────────────────────────────────────────

    @Test
    fun `the first step is the single cell of region A`() {
        val step = CrownsSolver.nextStep(CandidateGrid.fresh(puzzle))

        assertEquals(
            SolverStep(
                Technique.SINGLE_CANDIDATE,
                placements = setOf(cell(0, 0)),
                sources = listOf(Area(AreaType.REGION, 0))
            ),
            step
        )
    }

    @Test
    fun `region B confined to column 2 clears the rest of the column`() {
        val grid = CandidateGrid.fresh(puzzle).apply { place(cell(0, 0)) }

        val step = CrownsSolver.nextStep(grid)

        assertEquals(
            SolverStep(
                Technique.CONFINED_AREA,
                eliminations = setOf(cell(3, 2), cell(4, 2)),
                sources = listOf(Area(AreaType.REGION, 1)),
                targets = listOf(Area(AreaType.COLUMN, 2))
            ),
            step
        )
    }

    @Test
    fun `two regions inside two rows clear the rest of those rows`() {
        // Regions are the six columns. Crosses leave columns A and B only rows 0 and 1.
        val strips = parse("024135", "ABCDEF", "ABCDEF", "ABCDEF", "ABCDEF", "ABCDEF", "ABCDEF")
        val crosses = (2 until 6).flatMap { row -> listOf(strips.cellAt(row, 0), strips.cellAt(row, 1)) }
        val grid = CandidateGrid.fromBoard(strips, CrownsBoard.empty(6).withMarks(crosses, CellMark.CROSS))

        val step = Techniques.find(grid, Technique.GROUP)

        assertEquals(
            SolverStep(
                Technique.GROUP,
                eliminations = (0..1).flatMap { row -> (2 until 6).map { strips.cellAt(row, it) } }.toSet(),
                sources = listOf(Area(AreaType.REGION, 0), Area(AreaType.REGION, 1)),
                targets = listOf(Area(AreaType.ROW, 0), Area(AreaType.ROW, 1))
            ),
            step
        )
    }

    @Test
    fun `a technique that finds nothing returns null`() {
        // On a fresh grid of the strips puzzle no group of regions is confined.
        val strips = parse("024135", "ABCDEF", "ABCDEF", "ABCDEF", "ABCDEF", "ABCDEF", "ABCDEF")

        assertNull(Techniques.find(CandidateGrid.fresh(strips), Technique.GROUP))
    }

    @Test
    fun `the player's crosses and crowns feed the solver`() {
        val board = CrownsBoard.empty(5)
            .withMark(cell(0, 0), CellMark.CROWN)
            .withMark(cell(2, 2), CellMark.CROSS)
        val grid = CandidateGrid.fromBoard(puzzle, board)

        assertTrue(grid.hasCrown(cell(0, 0)))
        assertFalse(grid.isCandidate(cell(2, 2)))
        assertFalse(grid.isCandidate(cell(1, 1)))   // touches the crown
        assertTrue(grid.isCandidate(cell(1, 2)))
    }

    // ── Solution counter ────────────────────────────────────────────────────

    @Test
    fun `the hand-made grid has exactly one solution, the stored one`() {
        val solutions = CrownsSolver.findSolutions(puzzle, limit = 2)

        assertEquals(1, solutions.size)
        assertArrayEquals(puzzle.solution, solutions.single())
    }

    @Test
    fun `the counter stops at the limit on a grid with many solutions`() {
        assertEquals(2, CrownsSolver.countSolutions(rowsAsRegions, limit = 2))
    }
}
