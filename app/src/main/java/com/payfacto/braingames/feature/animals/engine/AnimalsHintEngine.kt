package com.payfacto.braingames.feature.animals.engine

import com.payfacto.braingames.feature.animals.model.AnimalsBoard
import com.payfacto.braingames.feature.animals.model.AnimalsPuzzle
import com.payfacto.braingames.feature.animals.model.CellMark

/** What a hint tells the player, as data; the UI turns it into localized text. */
sealed interface AnimalsHint {

    /** [mark] on [cell] is wrong: an animal where there is none, or a cross on an animal's cell. */
    data class WrongMark(val cell: Int, val mark: CellMark) : AnimalsHint

    /** The next logical step from the player's current marks. */
    data class Deduction(val step: SolverStep) : AnimalsHint

    /** Safety net when logic finds nothing: shows where one animal goes. Not expected for verified puzzles. */
    data class Reveal(val cell: Int) : AnimalsHint

    data object Solved : AnimalsHint
}

object AnimalsHintEngine {

    /**
     * Wrong marks come first, misplaced animals before crossed-out animal cells. Then the
     * easiest deduction from the player's own marks (question marks count as empty).
     * Never contradicts the solution: a step that would is replaced by a [AnimalsHint.Reveal].
     */
    fun nextHint(puzzle: AnimalsPuzzle, board: AnimalsBoard): AnimalsHint {
        AnimalsValidator.wrongMarks(puzzle, board).firstOrNull()?.let { cell ->
            return AnimalsHint.WrongMark(cell, board[cell])
        }
        if (AnimalsValidator.isSolved(puzzle, board)) return AnimalsHint.Solved

        val step = AnimalsSolver.nextStep(CandidateGrid.fromBoard(puzzle, board))
        if (step != null && agreesWithSolution(puzzle, step)) return AnimalsHint.Deduction(step)

        return AnimalsHint.Reveal(puzzle.solutionCells.first { board[it] != CellMark.ANIMAL })
    }

    private fun agreesWithSolution(puzzle: AnimalsPuzzle, step: SolverStep): Boolean {
        val solution = puzzle.solutionCells
        return solution.containsAll(step.placements) && step.eliminations.none { it in solution }
    }
}
