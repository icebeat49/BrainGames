package com.payfacto.braingames.feature.animals

import com.payfacto.braingames.feature.animals.AnimalsTestGrids.parse
import com.payfacto.braingames.feature.animals.AnimalsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.animals.engine.AnimalsValidator
import com.payfacto.braingames.feature.animals.engine.Conflict
import com.payfacto.braingames.feature.animals.engine.PuzzleProblem
import com.payfacto.braingames.feature.animals.engine.Rule
import com.payfacto.braingames.feature.animals.model.AnimalsBoard
import com.payfacto.braingames.feature.animals.model.CellMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalsValidatorTest {

    private val puzzle = uniqueFiveByFive
    private val empty = AnimalsBoard.empty(5)
    private fun cell(row: Int, col: Int) = puzzle.cellAt(row, col)
    private fun boardWithAnimals(vararg cells: Int) = empty.withMarks(cells.toList(), CellMark.ANIMAL)

    // ── Player boards ───────────────────────────────────────────────────────

    @Test
    fun `the solution is solved and has no conflicts`() {
        val board = empty.withMarks(puzzle.solutionCells, CellMark.ANIMAL)

        assertTrue(AnimalsValidator.isSolved(puzzle, board))
        assertEquals(emptyList<Conflict>(), AnimalsValidator.conflicts(puzzle, board))
    }

    @Test
    fun `crosses and question marks are ignored`() {
        val board = empty
            .withMarks(puzzle.solutionCells, CellMark.ANIMAL)
            .withMark(cell(0, 4), CellMark.CROSS)
            .withMark(cell(4, 0), CellMark.QUESTION)

        assertTrue(AnimalsValidator.isSolved(puzzle, board))
    }

    @Test
    fun `fewer than N animals is not solved`() {
        val board = boardWithAnimals(cell(0, 0), cell(1, 2))

        assertFalse(AnimalsValidator.isSolved(puzzle, board))
    }

    @Test
    fun `two animals in a row are reported as SAME_ROW`() {
        val conflicts = AnimalsValidator.conflicts(puzzle, boardWithAnimals(cell(1, 0), cell(1, 4)))

        assertEquals(listOf(Conflict(Rule.SAME_ROW, cell(1, 0), cell(1, 4))), conflicts)
    }

    @Test
    fun `two animals in a column are reported as SAME_COLUMN`() {
        // (0,4) region C and (4,4) region E.
        val conflicts = AnimalsValidator.conflicts(puzzle, boardWithAnimals(cell(0, 4), cell(4, 4)))

        assertEquals(listOf(Conflict(Rule.SAME_COLUMN, cell(0, 4), cell(4, 4))), conflicts)
    }

    @Test
    fun `two animals in a region are reported as SAME_REGION`() {
        // (2,1) and (4,0): both region D, different row and column, not touching.
        val conflicts = AnimalsValidator.conflicts(puzzle, boardWithAnimals(cell(2, 1), cell(4, 0)))

        assertEquals(listOf(Conflict(Rule.SAME_REGION, cell(2, 1), cell(4, 0))), conflicts)
    }

    @Test
    fun `diagonal neighbours are reported as TOUCHING`() {
        // (1,2) region B and (2,3) region C: different row, column and region.
        val conflicts = AnimalsValidator.conflicts(puzzle, boardWithAnimals(cell(1, 2), cell(2, 3)))

        assertEquals(listOf(Conflict(Rule.TOUCHING, cell(1, 2), cell(2, 3))), conflicts)
    }

    @Test
    fun `a pair breaking several rules is reported once per rule`() {
        // (0,1) and (0,2): same row, same region B, side by side.
        val rules = AnimalsValidator.conflicts(puzzle, boardWithAnimals(cell(0, 1), cell(0, 2))).map { it.rule }

        assertEquals(setOf(Rule.SAME_ROW, Rule.SAME_REGION, Rule.TOUCHING), rules.toSet())
    }

    @Test
    fun `wrong marks lists misplaced animals then crossed-out solution cells`() {
        val board = empty
            .withMark(cell(0, 0), CellMark.CROSS)   // solution cell crossed out
            .withMark(cell(4, 4), CellMark.ANIMAL)  // not a solution cell
            .withMark(cell(1, 2), CellMark.ANIMAL)  // correct

        assertEquals(listOf(cell(4, 4), cell(0, 0)), AnimalsValidator.wrongMarks(puzzle, board))
    }

    // ── Puzzle shape ────────────────────────────────────────────────────────

    @Test
    fun `a well-formed puzzle has no problems`() {
        assertEquals(emptySet<PuzzleProblem>(), AnimalsValidator.puzzleProblems(puzzle))
    }

    @Test
    fun `a region in two pieces is reported`() {
        val split = parse("02413", "ABBCC", "BBBCC", "DDBCC", "DDEEC", "EDEEE")

        assertTrue(PuzzleProblem.DISCONNECTED_REGION in AnimalsValidator.puzzleProblems(split))
    }

    @Test
    fun `a missing region is reported`() {
        val fourRegions = parse("02413", "ABBCC", "BBBCC", "DDBCC", "DDDDC", "DDDDD")

        assertTrue(PuzzleProblem.WRONG_REGION_COUNT in AnimalsValidator.puzzleProblems(fourRegions))
    }

    @Test
    fun `a solution that breaks a rule is reported`() {
        val touching = parse("01413", "ABBCC", "BBBCC", "DDBCC", "DDEEC", "DEEEE")

        assertTrue(PuzzleProblem.INVALID_SOLUTION in AnimalsValidator.puzzleProblems(touching))
    }
}
