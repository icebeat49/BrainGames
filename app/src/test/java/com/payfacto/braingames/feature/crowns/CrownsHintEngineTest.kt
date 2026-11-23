package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.CrownsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.crowns.data.CrownsFallbackPuzzles
import com.payfacto.braingames.feature.crowns.engine.CrownsGenerator
import com.payfacto.braingames.feature.crowns.engine.CrownsHint
import com.payfacto.braingames.feature.crowns.engine.CrownsHintEngine
import com.payfacto.braingames.feature.crowns.engine.CrownsValidator
import com.payfacto.braingames.feature.crowns.engine.Area
import com.payfacto.braingames.feature.crowns.engine.AreaType
import com.payfacto.braingames.feature.crowns.engine.SolverStep
import com.payfacto.braingames.feature.crowns.engine.Technique
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import com.payfacto.braingames.feature.crowns.model.CellMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class CrownsHintEngineTest {

    private val puzzle = uniqueFiveByFive
    private val empty = CrownsBoard.empty(5)
    private fun cell(row: Int, col: Int) = puzzle.cellAt(row, col)

    // ── Wrong marks first ───────────────────────────────────────────────────

    @Test
    fun `a misplaced crown is pointed out first`() {
        val board = empty
            .withMark(cell(4, 4), CellMark.CROWN)
            .withMark(cell(0, 0), CellMark.CROSS)

        assertEquals(CrownsHint.WrongMark(cell(4, 4), CellMark.CROWN), CrownsHintEngine.nextHint(puzzle, board))
    }

    @Test
    fun `a cross on a crown's cell is pointed out`() {
        val board = empty.withMark(cell(1, 2), CellMark.CROSS)

        assertEquals(CrownsHint.WrongMark(cell(1, 2), CellMark.CROSS), CrownsHintEngine.nextHint(puzzle, board))
    }

    @Test
    fun `a question mark is never treated as wrong`() {
        val board = empty.withMark(cell(4, 4), CellMark.QUESTION)

        assertTrue(CrownsHintEngine.nextHint(puzzle, board) is CrownsHint.Deduction)
    }

    // ── Deductions ──────────────────────────────────────────────────────────

    @Test
    fun `on an empty board the hint is the single cell of region A`() {
        assertEquals(
            CrownsHint.Deduction(
                SolverStep(
                    Technique.SINGLE_CANDIDATE,
                    placements = setOf(cell(0, 0)),
                    sources = listOf(Area(AreaType.REGION, 0))
                )
            ),
            CrownsHintEngine.nextHint(puzzle, empty)
        )
    }

    @Test
    fun `the hint builds on the player's own marks`() {
        val board = empty.withMark(cell(0, 0), CellMark.CROWN)

        val hint = CrownsHintEngine.nextHint(puzzle, board) as CrownsHint.Deduction

        assertEquals(Technique.CONFINED_AREA, hint.step.technique)
    }

    @Test
    fun `a solved board says so`() {
        val board = empty.withMarks(puzzle.solutionCells, CellMark.CROWN)

        assertEquals(CrownsHint.Solved, CrownsHintEngine.nextHint(puzzle, board))
    }

    // ── Following hints ─────────────────────────────────────────────────────

    @Test
    fun `following hints from an empty board solves the hand-made grid`() {
        assertHintsSolve(puzzle, empty, "hand-made")
    }

    @Test
    fun `following hints solves every fallback puzzle`() {
        Difficulty.entries.forEach { difficulty ->
            CrownsFallbackPuzzles.all(difficulty).forEachIndexed { i, p ->
                assertHintsSolve(p, CrownsBoard.empty(p.size), "fallback $difficulty #$i")
            }
        }
    }

    @Test
    fun `following hints solves generated puzzles of every difficulty`() {
        Difficulty.entries.forEach { difficulty ->
            val generator = CrownsGenerator(Random(500L + difficulty.ordinal))
            repeat(GENERATED_PER_DIFFICULTY) { i ->
                val p = checkNotNull(generator.generate(difficulty))
                assertHintsSolve(p, CrownsBoard.empty(p.size), "generated $difficulty #$i")
            }
        }
    }

    @Test
    fun `hints recover from a messy board with mistakes and question marks`() {
        val random = Random(7)
        val generator = CrownsGenerator(Random(8))
        repeat(GENERATED_PER_DIFFICULTY) { i ->
            val p = checkNotNull(generator.generate(Difficulty.HARD))
            assertHintsSolve(p, messyBoard(p, random), "messy #$i")
        }
    }

    /**
     * Applies hints until solved. Every deduction must agree with the solution, the
     * safety-net Reveal must never be needed, and the loop must end.
     */
    private fun assertHintsSolve(puzzle: CrownsPuzzle, start: CrownsBoard, label: String) {
        var board = start
        repeat(puzzle.cellCount * 3) {
            when (val hint = CrownsHintEngine.nextHint(puzzle, board)) {
                CrownsHint.Solved -> {
                    assertTrue("$label: Solved but not solved", CrownsValidator.isSolved(puzzle, board))
                    return
                }
                is CrownsHint.WrongMark -> board = board.withMark(hint.cell, CellMark.EMPTY)
                is CrownsHint.Deduction -> {
                    val step = hint.step
                    assertTrue("$label: wrong placement $step", puzzle.solutionCells.containsAll(step.placements))
                    assertTrue("$label: solution crossed out $step", step.eliminations.none { it in puzzle.solutionCells })
                    board = board.withMarks(step.eliminations, CellMark.CROSS).withMarks(step.placements, CellMark.CROWN)
                }
                is CrownsHint.Reveal -> fail("$label: logic got stuck and had to reveal ${hint.cell}")
            }
        }
        fail("$label: hints did not reach the solution")
    }

    /** Random marks: some right, some wrong crowns and crosses, some question marks. */
    private fun messyBoard(puzzle: CrownsPuzzle, random: Random): CrownsBoard {
        var board = CrownsBoard.empty(puzzle.size)
        repeat(puzzle.size * 2) {
            val cell = random.nextInt(puzzle.cellCount)
            val mark = listOf(CellMark.CROSS, CellMark.QUESTION, CellMark.CROWN).random(random)
            board = board.withMark(cell, mark)
        }
        return board
    }

    private companion object {
        const val GENERATED_PER_DIFFICULTY = 20
    }
}
