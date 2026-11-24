package com.payfacto.braingames.feature.crowns.engine

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import kotlin.math.abs
import kotlin.random.Random

/**
 * Builds puzzles with exactly one solution that the logical solver can reach without guessing,
 * at the requested difficulty and within its [RegionRules].
 *
 * 0. Decide once per puzzle, at random with [RegionRules.singleCellChance], whether it gets
 *    one 1-cell region; retries keep that decision, so the frequency matches the target.
 * 1. Place N non-touching crowns: a random permutation with no two consecutive rows in
 *    neighbouring columns. This is the solution.
 * 2. Grow N regions from those cells, always extending the smallest region that can grow
 *    ([RegionLayout.growBalanced]); then bring any undersized region back to the minimum by
 *    taking border cells from larger neighbours ([RegionLayout.fixSmallRegions]).
 * 3. Repair until accepted, at most [maxRepairs] times:
 *    - a second solution exists → move one of its crown cells into a neighbouring region,
 *      which then holds two of its crowns, so that solution dies (ours is untouched);
 *    - unique but not solvable by logic, wrong difficulty or too many immediate crowns →
 *      move a random border cell.
 *    Moves never touch solution cells, never split a region and never break the region rules.
 * 4. Start again with a new layout, at most [maxLayouts] times; then give up (null).
 *
 * Pure and single-threaded: call it from Dispatchers.Default.
 */
class CrownsGenerator(
    private val random: Random = Random.Default,
    private val maxLayouts: Int = DEFAULT_MAX_LAYOUTS,
    private val maxRepairs: Int = DEFAULT_MAX_REPAIRS
) {

    fun generate(difficulty: Difficulty): CrownsPuzzle? {
        val size = CrownsDifficulty.sizeFor(difficulty)
        val rules = CrownsGenerationRules.forDifficulty(difficulty)
        val withSingleCell = random.nextDouble() < rules.singleCellChance
        repeat(maxLayouts) {
            val solution = randomSolution(size)
            val layout = regionLayout(size, solution, rules, withSingleCell) ?: return@repeat
            repair(layout, size, solution, difficulty, rules)?.let { return it }
        }
        return null
    }

    private fun regionLayout(size: Int, solution: IntArray, rules: RegionRules, withSingleCell: Boolean): RegionLayout? {
        val seeds = IntArray(size) { row -> row * size + solution[row] }
        // Region ids are shuffled so that region 0 is not always the top row's region.
        val ids = (0 until size).shuffled(random)
        val seedOfRegion = IntArray(size).also { bySeed -> ids.forEachIndexed { row, id -> bySeed[id] = seeds[row] } }
        val singleCellRegion = if (withSingleCell) random.nextInt(size) else null
        val layout = RegionLayout(size, seedOfRegion, rules, singleCellRegion)
        return layout.takeIf { it.growBalanced(random) && it.fixSmallRegions(random) }
    }

    private fun repair(
        layout: RegionLayout,
        size: Int,
        solution: IntArray,
        difficulty: Difficulty,
        rules: RegionRules
    ): CrownsPuzzle? {
        val solutionCells = solution.withIndex().map { (row, col) -> row * size + col }.toSet()
        repeat(maxRepairs) {
            val puzzle = CrownsPuzzle(size, layout.regions(), solution)
            val alternative = CrownsSolver.findSolutions(puzzle, limit = 2)
                .firstOrNull { !it.contentEquals(solution) }
            val moved = if (alternative != null) {
                val alternativeCells = alternative.withIndex().map { (row, col) -> row * size + col }
                breakAlternative(layout, alternativeCells - solutionCells) || moveRandomBorderCell(layout)
            } else {
                if (isAccepted(puzzle, difficulty, rules)) return puzzle
                moveRandomBorderCell(layout)
            }
            if (!moved) return null
        }
        return null
    }

    private fun isAccepted(puzzle: CrownsPuzzle, difficulty: Difficulty, rules: RegionRules): Boolean {
        val result = CrownsSolver.solve(puzzle)
        return result.solved &&
            CrownsDifficulty.accepts(difficulty, result.hardest) &&
            CrownsSolver.immediateCrowns(puzzle) <= rules.maxImmediateCrowns
    }

    // ── Step 1: solution ────────────────────────────────────────────────────

    private fun randomSolution(size: Int): IntArray {
        val columns = IntArray(size)
        val used = BooleanArray(size)

        fun placeRow(row: Int): Boolean {
            if (row == size) return true
            for (col in (0 until size).shuffled(random)) {
                if (used[col] || (row > 0 && abs(col - columns[row - 1]) <= 1)) continue
                columns[row] = col
                used[col] = true
                if (placeRow(row + 1)) return true
                used[col] = false
            }
            return false
        }

        check(placeRow(0)) { "no non-touching layout exists for size $size" }
        return columns
    }

    // ── Step 3: repairs ─────────────────────────────────────────────────────

    /**
     * Moves one of the alternative solution's own cells into a neighbouring region. Every
     * region holds exactly one crown of that solution, so the receiving region now holds
     * two and the alternative is no longer valid.
     */
    private fun breakAlternative(layout: RegionLayout, alternativeOnlyCells: List<Int>): Boolean =
        alternativeOnlyCells.shuffled(random).any { cell -> layout.moveToNeighbourRegion(cell, random) }

    private fun moveRandomBorderCell(layout: RegionLayout): Boolean =
        layout.regions().indices.shuffled(random).any { cell -> layout.moveToNeighbourRegion(cell, random) }

    companion object {
        const val DEFAULT_MAX_LAYOUTS = 100
        const val DEFAULT_MAX_REPAIRS = 200
    }
}
