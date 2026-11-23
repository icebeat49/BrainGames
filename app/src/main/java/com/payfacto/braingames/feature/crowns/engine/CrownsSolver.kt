package com.payfacto.braingames.feature.crowns.engine

import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import kotlin.math.abs

/**
 * @param solved every crown was placed by logic alone.
 * @param hardest the hardest technique needed, null if no step was taken.
 * @param steps the deductions in the order they were applied.
 */
data class SolveResult(
    val solved: Boolean,
    val hardest: Technique?,
    val steps: List<SolverStep>
)

object CrownsSolver {

    /** The easiest deduction available on [grid], or null if logic is stuck. */
    fun nextStep(grid: CandidateGrid): SolverStep? =
        Technique.entries.firstNotNullOfOrNull { Techniques.find(grid, it) }

    /**
     * Solves like a human: repeatedly applies the easiest technique that makes progress.
     * Never guesses, so a puzzle with several solutions is never reported as solved.
     */
    fun solve(puzzle: CrownsPuzzle, start: CandidateGrid = CandidateGrid.fresh(puzzle)): SolveResult {
        val grid = start.copy()
        val steps = mutableListOf<SolverStep>()
        while (!grid.isSolved && !grid.isContradictory) {
            val step = nextStep(grid) ?: break
            grid.apply(step)
            steps += step
        }
        return SolveResult(
            solved = grid.isSolved,
            hardest = steps.maxOfOrNull { it.technique },
            steps = steps
        )
    }

    /** Number of solutions, counting no further than [limit]. 1 means the puzzle is unique. */
    fun countSolutions(puzzle: CrownsPuzzle, limit: Int = 2): Int = findSolutions(puzzle, limit).size

    /**
     * Up to [limit] solutions by backtracking, each as one column per row. Row by row:
     * a column, a region and the previous row's neighbourhood can each be used once.
     */
    fun findSolutions(puzzle: CrownsPuzzle, limit: Int = 2): List<IntArray> {
        val n = puzzle.size
        val columns = IntArray(n)
        val columnUsed = BooleanArray(n)
        val regionUsed = BooleanArray(n)
        val found = mutableListOf<IntArray>()

        fun placeRow(row: Int) {
            if (row == n) {
                found += columns.copyOf()
                return
            }
            for (col in 0 until n) {
                if (found.size >= limit) return
                if (columnUsed[col]) continue
                if (row > 0 && abs(col - columns[row - 1]) <= 1) continue
                val region = puzzle.regionOf(puzzle.cellAt(row, col))
                if (regionUsed[region]) continue
                columns[row] = col
                columnUsed[col] = true
                regionUsed[region] = true
                placeRow(row + 1)
                columnUsed[col] = false
                regionUsed[region] = false
            }
        }

        placeRow(0)
        return found
    }
}
