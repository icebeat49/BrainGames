package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.feature.crowns.CrownsTestGrids.parse
import com.payfacto.braingames.feature.crowns.CrownsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.crowns.engine.CrownsValidator
import com.payfacto.braingames.feature.crowns.engine.Conflict
import com.payfacto.braingames.feature.crowns.engine.PuzzleProblem
import com.payfacto.braingames.feature.crowns.engine.Rule
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CellMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrownsValidatorTest {

    private val puzzle = uniqueFiveByFive
    private val empty = CrownsBoard.empty(5)
    private fun cell(row: Int, col: Int) = puzzle.cellAt(row, col)
    private fun boardWithCrowns(vararg cells: Int) = empty.withMarks(cells.toList(), CellMark.CROWN)

    // ── Player boards ───────────────────────────────────────────────────────

    @Test
    fun `the solution is solved and has no conflicts`() {
        val board = empty.withMarks(puzzle.solutionCells, CellMark.CROWN)

        assertTrue(CrownsValidator.isSolved(puzzle, board))
        assertEquals(emptyList<Conflict>(), CrownsValidator.conflicts(puzzle, board))
    }

    @Test
    fun `crosses and question marks are ignored`() {
        val board = empty
            .withMarks(puzzle.solutionCells, CellMark.CROWN)
            .withMark(cell(0, 4), CellMark.CROSS)
            .withMark(cell(4, 0), CellMark.QUESTION)

        assertTrue(CrownsValidator.isSolved(puzzle, board))
    }

    @Test
    fun `fewer than N crowns is not solved`() {
        val board = boardWithCrowns(cell(0, 0), cell(1, 2))

        assertFalse(CrownsValidator.isSolved(puzzle, board))
    }

    @Test
    fun `two crowns in a row are reported as SAME_ROW`() {
        val conflicts = CrownsValidator.conflicts(puzzle, boardWithCrowns(cell(1, 0), cell(1, 4)))

        assertEquals(listOf(Conflict(Rule.SAME_ROW, cell(1, 0), cell(1, 4))), conflicts)
    }

    @Test
    fun `two crowns in a column are reported as SAME_COLUMN`() {
        // (0,4) region C and (4,4) region E.
        val conflicts = CrownsValidator.conflicts(puzzle, boardWithCrowns(cell(0, 4), cell(4, 4)))

        assertEquals(listOf(Conflict(Rule.SAME_COLUMN, cell(0, 4), cell(4, 4))), conflicts)
    }

    @Test
    fun `two crowns in a region are reported as SAME_REGION`() {
        // (2,1) and (4,0): both region D, different row and column, not touching.
        val conflicts = CrownsValidator.conflicts(puzzle, boardWithCrowns(cell(2, 1), cell(4, 0)))

        assertEquals(listOf(Conflict(Rule.SAME_REGION, cell(2, 1), cell(4, 0))), conflicts)
    }

    @Test
    fun `diagonal neighbours are reported as TOUCHING`() {
        // (1,2) region B and (2,3) region C: different row, column and region.
        val conflicts = CrownsValidator.conflicts(puzzle, boardWithCrowns(cell(1, 2), cell(2, 3)))

        assertEquals(listOf(Conflict(Rule.TOUCHING, cell(1, 2), cell(2, 3))), conflicts)
    }

    @Test
    fun `a pair breaking several rules is reported once per rule`() {
        // (0,1) and (0,2): same row, same region B, side by side.
        val rules = CrownsValidator.conflicts(puzzle, boardWithCrowns(cell(0, 1), cell(0, 2))).map { it.rule }

        assertEquals(setOf(Rule.SAME_ROW, Rule.SAME_REGION, Rule.TOUCHING), rules.toSet())
    }

    @Test
    fun `wrong marks lists misplaced crowns then crossed-out solution cells`() {
        val board = empty
            .withMark(cell(0, 0), CellMark.CROSS)   // solution cell crossed out
            .withMark(cell(4, 4), CellMark.CROWN)  // not a solution cell
            .withMark(cell(1, 2), CellMark.CROWN)  // correct

        assertEquals(listOf(cell(4, 4), cell(0, 0)), CrownsValidator.wrongMarks(puzzle, board))
    }

    // ── Puzzle shape ────────────────────────────────────────────────────────

    @Test
    fun `a well-formed puzzle has no problems`() {
        assertEquals(emptySet<PuzzleProblem>(), CrownsValidator.puzzleProblems(puzzle))
    }

    @Test
    fun `a region in two pieces is reported`() {
        val split = parse("02413", "ABBCC", "BBBCC", "DDBCC", "DDEEC", "EDEEE")

        assertTrue(PuzzleProblem.DISCONNECTED_REGION in CrownsValidator.puzzleProblems(split))
    }

    @Test
    fun `a missing region is reported`() {
        val fourRegions = parse("02413", "ABBCC", "BBBCC", "DDBCC", "DDDDC", "DDDDD")

        assertTrue(PuzzleProblem.WRONG_REGION_COUNT in CrownsValidator.puzzleProblems(fourRegions))
    }

    @Test
    fun `a solution that breaks a rule is reported`() {
        val touching = parse("01413", "ABBCC", "BBBCC", "DDBCC", "DDEEC", "DEEEE")

        assertTrue(PuzzleProblem.INVALID_SOLUTION in CrownsValidator.puzzleProblems(touching))
    }
}
