package com.payfacto.braingames.feature.animals

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.animals.AnimalsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.animals.data.AnimalsFallbackPuzzles
import com.payfacto.braingames.feature.animals.engine.AnimalsGenerator
import com.payfacto.braingames.feature.animals.engine.AnimalsHint
import com.payfacto.braingames.feature.animals.engine.AnimalsHintEngine
import com.payfacto.braingames.feature.animals.engine.AnimalsValidator
import com.payfacto.braingames.feature.animals.engine.Area
import com.payfacto.braingames.feature.animals.engine.AreaType
import com.payfacto.braingames.feature.animals.engine.SolverStep
import com.payfacto.braingames.feature.animals.engine.Technique
import com.payfacto.braingames.feature.animals.model.AnimalsBoard
import com.payfacto.braingames.feature.animals.model.AnimalsPuzzle
import com.payfacto.braingames.feature.animals.model.CellMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class AnimalsHintEngineTest {

    private val puzzle = uniqueFiveByFive
    private val empty = AnimalsBoard.empty(5)
    private fun cell(row: Int, col: Int) = puzzle.cellAt(row, col)

    // ── Wrong marks first ───────────────────────────────────────────────────

    @Test
    fun `a misplaced animal is pointed out first`() {
        val board = empty
            .withMark(cell(4, 4), CellMark.ANIMAL)
            .withMark(cell(0, 0), CellMark.CROSS)

        assertEquals(AnimalsHint.WrongMark(cell(4, 4), CellMark.ANIMAL), AnimalsHintEngine.nextHint(puzzle, board))
    }

    @Test
    fun `a cross on an animal's cell is pointed out`() {
        val board = empty.withMark(cell(1, 2), CellMark.CROSS)

        assertEquals(AnimalsHint.WrongMark(cell(1, 2), CellMark.CROSS), AnimalsHintEngine.nextHint(puzzle, board))
    }

    @Test
    fun `a question mark is never treated as wrong`() {
        val board = empty.withMark(cell(4, 4), CellMark.QUESTION)

        assertTrue(AnimalsHintEngine.nextHint(puzzle, board) is AnimalsHint.Deduction)
    }

    // ── Deductions ──────────────────────────────────────────────────────────

    @Test
    fun `on an empty board the hint is the single cell of region A`() {
        assertEquals(
            AnimalsHint.Deduction(
                SolverStep(
                    Technique.SINGLE_CANDIDATE,
                    placements = setOf(cell(0, 0)),
                    sources = listOf(Area(AreaType.REGION, 0))
                )
            ),
            AnimalsHintEngine.nextHint(puzzle, empty)
        )
    }

    @Test
    fun `the hint builds on the player's own marks`() {
        val board = empty.withMark(cell(0, 0), CellMark.ANIMAL)

        val hint = AnimalsHintEngine.nextHint(puzzle, board) as AnimalsHint.Deduction

        assertEquals(Technique.CONFINED_AREA, hint.step.technique)
    }

    @Test
    fun `a solved board says so`() {
        val board = empty.withMarks(puzzle.solutionCells, CellMark.ANIMAL)

        assertEquals(AnimalsHint.Solved, AnimalsHintEngine.nextHint(puzzle, board))
    }

    // ── Following hints ─────────────────────────────────────────────────────

    @Test
    fun `following hints from an empty board solves the hand-made grid`() {
        assertHintsSolve(puzzle, empty, "hand-made")
    }

    @Test
    fun `following hints solves every fallback puzzle`() {
        Difficulty.entries.forEach { difficulty ->
            AnimalsFallbackPuzzles.all(difficulty).forEachIndexed { i, p ->
                assertHintsSolve(p, AnimalsBoard.empty(p.size), "fallback $difficulty #$i")
            }
        }
    }

    @Test
    fun `following hints solves generated puzzles of every difficulty`() {
        Difficulty.entries.forEach { difficulty ->
            val generator = AnimalsGenerator(Random(500L + difficulty.ordinal))
            repeat(GENERATED_PER_DIFFICULTY) { i ->
                val p = checkNotNull(generator.generate(difficulty))
                assertHintsSolve(p, AnimalsBoard.empty(p.size), "generated $difficulty #$i")
            }
        }
    }

    @Test
    fun `hints recover from a messy board with mistakes and question marks`() {
        val random = Random(7)
        val generator = AnimalsGenerator(Random(8))
        repeat(GENERATED_PER_DIFFICULTY) { i ->
            val p = checkNotNull(generator.generate(Difficulty.HARD))
            assertHintsSolve(p, messyBoard(p, random), "messy #$i")
        }
    }

    /**
     * Applies hints until solved. Every deduction must agree with the solution, the
     * safety-net Reveal must never be needed, and the loop must end.
     */
    private fun assertHintsSolve(puzzle: AnimalsPuzzle, start: AnimalsBoard, label: String) {
        var board = start
        repeat(puzzle.cellCount * 3) {
            when (val hint = AnimalsHintEngine.nextHint(puzzle, board)) {
                AnimalsHint.Solved -> {
                    assertTrue("$label: Solved but not solved", AnimalsValidator.isSolved(puzzle, board))
                    return
                }
                is AnimalsHint.WrongMark -> board = board.withMark(hint.cell, CellMark.EMPTY)
                is AnimalsHint.Deduction -> {
                    val step = hint.step
                    assertTrue("$label: wrong placement $step", puzzle.solutionCells.containsAll(step.placements))
                    assertTrue("$label: solution crossed out $step", step.eliminations.none { it in puzzle.solutionCells })
                    board = board.withMarks(step.eliminations, CellMark.CROSS).withMarks(step.placements, CellMark.ANIMAL)
                }
                is AnimalsHint.Reveal -> fail("$label: logic got stuck and had to reveal ${hint.cell}")
            }
        }
        fail("$label: hints did not reach the solution")
    }

    /** Random marks: some right, some wrong animals and crosses, some question marks. */
    private fun messyBoard(puzzle: AnimalsPuzzle, random: Random): AnimalsBoard {
        var board = AnimalsBoard.empty(puzzle.size)
        repeat(puzzle.size * 2) {
            val cell = random.nextInt(puzzle.cellCount)
            val mark = listOf(CellMark.CROSS, CellMark.QUESTION, CellMark.ANIMAL).random(random)
            board = board.withMark(cell, mark)
        }
        return board
    }

    private companion object {
        const val GENERATED_PER_DIFFICULTY = 20
    }
}
