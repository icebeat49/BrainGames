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

    /**
     * How many crowns fall out of the empty board with the simplest technique alone: apply
     * [Technique.SINGLE_CANDIDATE] until it finds nothing, before any other deduction.
     * From an empty board this only starts at a 1-cell region, then follows the chain it opens.
     */
    fun immediateCrowns(puzzle: CrownsPuzzle): Int {
        val grid = CandidateGrid.fresh(puzzle)
        while (!grid.isSolved && !grid.isContradictory) {
            val step = Techniques.find(grid, Technique.SINGLE_CANDIDATE) ?: break
            grid.apply(step)
        }
        return grid.crownCount
    }

    /** Number of solutions, counting no further than [limit]. 1 means the puzzle is unique. */
    fun countSolutions(puzzle: CrownsPuzzle, limit: Int = 2): Int = findSolutions(puzzle, limit).size

    /**
     * Up to [limit] solutions by backtracking, each as one column per row. Row by row:
     * a column, a region and the previous row's neighbourhood can each be used once.
     * A region whose last row is this one and still has no crown must take it here; two such
     * regions end the branch.
     */
    fun findSolutions(puzzle: CrownsPuzzle, limit: Int = 2): List<IntArray> {
        val n = puzzle.size
        val columns = IntArray(n)
        val columnUsed = BooleanArray(n)
        val regionUsed = BooleanArray(n)
        val lastRowOf = IntArray(n)
        for (cell in 0 until puzzle.cellCount) {
            val region = puzzle.regionOf(cell)
            lastRowOf[region] = maxOf(lastRowOf[region], puzzle.rowOf(cell))
        }
        val found = mutableListOf<IntArray>()

        /** The region that must take this row's crown, NONE if free, or DEAD_END if two must. */
        fun forcedRegion(row: Int): Int {
            var forced = NONE
            for (region in 0 until n) {
                if (regionUsed[region] || lastRowOf[region] != row) continue
                if (forced != NONE) return DEAD_END
                forced = region
            }
            return forced
        }

        fun placeRow(row: Int) {
            if (row == n) {
                found += columns.copyOf()
                return
            }
            val forced = forcedRegion(row)
            if (forced == DEAD_END) return
            for (col in 0 until n) {
                if (found.size >= limit) return
                if (columnUsed[col]) continue
                if (row > 0 && abs(col - columns[row - 1]) <= 1) continue
                val region = puzzle.regionOf(puzzle.cellAt(row, col))
                if (regionUsed[region] || (forced != NONE && region != forced)) continue
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

    private const val NONE = -1
    private const val DEAD_END = -2
}
