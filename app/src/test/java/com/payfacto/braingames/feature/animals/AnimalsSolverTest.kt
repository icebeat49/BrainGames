package com.payfacto.braingames.feature.animals

import com.payfacto.braingames.feature.animals.AnimalsTestGrids.parse
import com.payfacto.braingames.feature.animals.AnimalsTestGrids.rowsAsRegions
import com.payfacto.braingames.feature.animals.AnimalsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.animals.engine.AnimalsSolver
import com.payfacto.braingames.feature.animals.engine.Area
import com.payfacto.braingames.feature.animals.engine.AreaType
import com.payfacto.braingames.feature.animals.engine.CandidateGrid
import com.payfacto.braingames.feature.animals.engine.SolverStep
import com.payfacto.braingames.feature.animals.engine.Technique
import com.payfacto.braingames.feature.animals.engine.Techniques
import com.payfacto.braingames.feature.animals.model.AnimalsBoard
import com.payfacto.braingames.feature.animals.model.CellMark
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalsSolverTest {

    private val puzzle = uniqueFiveByFive
    private fun cell(row: Int, col: Int) = puzzle.cellAt(row, col)

    // ── Full solve ──────────────────────────────────────────────────────────

    @Test
    fun `the hand-made grid is solved by logic alone`() {
        val result = AnimalsSolver.solve(puzzle)

        assertTrue(result.solved)
        assertEquals(puzzle.solutionCells, result.steps.flatMap { it.placements }.toSet())
    }

    @Test
    fun `the hand-made grid needs a neighbour elimination`() {
        assertEquals(Technique.NEIGHBOUR_ELIMINATION, AnimalsSolver.solve(puzzle).hardest)
    }

    @Test
    fun `no step ever crosses out a solution cell or places a wrong animal`() {
        val steps = AnimalsSolver.solve(puzzle).steps

        assertTrue(steps.all { puzzle.solutionCells.containsAll(it.placements) })
        assertTrue(steps.none { step -> step.eliminations.any { it in puzzle.solutionCells } })
    }

    @Test
    fun `a puzzle with several solutions is not solved by logic`() {
        assertFalse(AnimalsSolver.solve(rowsAsRegions).solved)
    }

    // ── Individual techniques ───────────────────────────────────────────────

    @Test
    fun `the first step is the single cell of region A`() {
        val step = AnimalsSolver.nextStep(CandidateGrid.fresh(puzzle))

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

        val step = AnimalsSolver.nextStep(grid)

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
        val grid = CandidateGrid.fromBoard(strips, AnimalsBoard.empty(6).withMarks(crosses, CellMark.CROSS))

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
    fun `the player's crosses and animals feed the solver`() {
        val board = AnimalsBoard.empty(5)
            .withMark(cell(0, 0), CellMark.ANIMAL)
            .withMark(cell(2, 2), CellMark.CROSS)
        val grid = CandidateGrid.fromBoard(puzzle, board)

        assertTrue(grid.hasAnimal(cell(0, 0)))
        assertFalse(grid.isCandidate(cell(2, 2)))
        assertFalse(grid.isCandidate(cell(1, 1)))   // touches the animal
        assertTrue(grid.isCandidate(cell(1, 2)))
    }

    // ── Solution counter ────────────────────────────────────────────────────

    @Test
    fun `the hand-made grid has exactly one solution, the stored one`() {
        val solutions = AnimalsSolver.findSolutions(puzzle, limit = 2)

        assertEquals(1, solutions.size)
        assertArrayEquals(puzzle.solution, solutions.single())
    }

    @Test
    fun `the counter stops at the limit on a grid with many solutions`() {
        assertEquals(2, AnimalsSolver.countSolutions(rowsAsRegions, limit = 2))
    }
}
