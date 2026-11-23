package com.payfacto.braingames.feature.crowns.engine

import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import com.payfacto.braingames.feature.crowns.model.CellMark

/** What a hint tells the player, as data; the UI turns it into localized text. */
sealed interface CrownsHint {

    /** [mark] on [cell] is wrong: a crown where there is none, or a cross on a crown's cell. */
    data class WrongMark(val cell: Int, val mark: CellMark) : CrownsHint

    /** The next logical step from the player's current marks. */
    data class Deduction(val step: SolverStep) : CrownsHint

    /** Safety net when logic finds nothing: shows where one crown goes. Not expected for verified puzzles. */
    data class Reveal(val cell: Int) : CrownsHint

    data object Solved : CrownsHint
}

object CrownsHintEngine {

    /**
     * Wrong marks come first, misplaced crowns before crossed-out crown cells. Then the
     * easiest deduction from the player's own marks (question marks count as empty).
     * Never contradicts the solution: a step that would is replaced by a [CrownsHint.Reveal].
     */
    fun nextHint(puzzle: CrownsPuzzle, board: CrownsBoard): CrownsHint {
        CrownsValidator.wrongMarks(puzzle, board).firstOrNull()?.let { cell ->
            return CrownsHint.WrongMark(cell, board[cell])
        }
        if (CrownsValidator.isSolved(puzzle, board)) return CrownsHint.Solved

        val step = CrownsSolver.nextStep(CandidateGrid.fromBoard(puzzle, board))
        if (step != null && agreesWithSolution(puzzle, step)) return CrownsHint.Deduction(step)

        return CrownsHint.Reveal(puzzle.solutionCells.first { board[it] != CellMark.CROWN })
    }

    private fun agreesWithSolution(puzzle: CrownsPuzzle, step: SolverStep): Boolean {
        val solution = puzzle.solutionCells
        return solution.containsAll(step.placements) && step.eliminations.none { it in solution }
    }
}
