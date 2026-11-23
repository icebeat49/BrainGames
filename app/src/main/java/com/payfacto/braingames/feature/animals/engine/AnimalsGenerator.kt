package com.payfacto.braingames.feature.animals.engine

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.animals.model.AnimalsPuzzle
import kotlin.math.abs
import kotlin.random.Random

/**
 * Builds puzzles with exactly one solution that the logical solver can reach without guessing,
 * at the requested difficulty.
 *
 * 1. Place N non-touching animals: a random permutation with no two consecutive rows in
 *    neighbouring columns. This is the solution.
 * 2. Grow N regions from those cells by randomised flood fill.
 * 3. Repair until accepted, at most [maxRepairs] times:
 *    - a second solution exists → move one of its animal cells into a neighbouring region,
 *      which then holds two of its animals, so that solution dies (ours is untouched);
 *    - unique but not solvable by logic, or wrong difficulty → move a random border cell.
 *    Moves never touch solution cells and never split a region.
 * 4. Start again with a new layout, at most [maxLayouts] times; then give up (null).
 *
 * Pure and single-threaded: call it from Dispatchers.Default.
 */
class AnimalsGenerator(
    private val random: Random = Random.Default,
    private val maxLayouts: Int = DEFAULT_MAX_LAYOUTS,
    private val maxRepairs: Int = DEFAULT_MAX_REPAIRS
) {

    fun generate(difficulty: Difficulty): AnimalsPuzzle? {
        val size = AnimalsDifficulty.sizeFor(difficulty)
        repeat(maxLayouts) {
            val solution = randomSolution(size)
            val regions = growRegions(size, solution)
            repair(size, regions, solution, difficulty)?.let { return it }
        }
        return null
    }

    private fun repair(size: Int, regions: IntArray, solution: IntArray, difficulty: Difficulty): AnimalsPuzzle? {
        val solutionCells = solution.withIndex().map { (row, col) -> row * size + col }.toSet()
        repeat(maxRepairs) {
            val puzzle = AnimalsPuzzle(size, regions, solution)
            val alternative = AnimalsSolver.findSolutions(puzzle, limit = 2)
                .firstOrNull { !it.contentEquals(solution) }
            val moved = if (alternative != null) {
                val alternativeCells = alternative.withIndex().map { (row, col) -> row * size + col }
                breakAlternative(size, regions, alternativeCells - solutionCells) ||
                    moveRandomBorderCell(size, regions, solutionCells)
            } else {
                val result = AnimalsSolver.solve(puzzle)
                if (result.solved && AnimalsDifficulty.accepts(difficulty, result.hardest)) return puzzle
                moveRandomBorderCell(size, regions, solutionCells)
            }
            if (!moved) return null
        }
        return null
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

    // ── Step 2: regions ─────────────────────────────────────────────────────

    /** Each solution cell seeds a region; unassigned cells next to a region join it at random. */
    private fun growRegions(size: Int, solution: IntArray): IntArray {
        val regions = IntArray(size * size) { UNASSIGNED }
        val ids = (0 until size).shuffled(random)
        solution.forEachIndexed { row, col -> regions[row * size + col] = ids[row] }

        var unassigned = size * size - size
        while (unassigned > 0) {
            val frontier = regions.indices.filter { cell ->
                regions[cell] == UNASSIGNED && edgeNeighbours(size, cell).any { regions[it] != UNASSIGNED }
            }
            val cell = frontier.random(random)
            val joined = edgeNeighbours(size, cell).filter { regions[it] != UNASSIGNED }.random(random)
            regions[cell] = regions[joined]
            unassigned--
        }
        return regions
    }

    // ── Step 3: repairs ─────────────────────────────────────────────────────

    /**
     * Moves one of the alternative solution's own cells into a neighbouring region. Every
     * region holds exactly one animal of that solution, so the receiving region now holds
     * two and the alternative is no longer valid.
     */
    private fun breakAlternative(size: Int, regions: IntArray, alternativeOnlyCells: List<Int>): Boolean =
        alternativeOnlyCells.shuffled(random).any { cell -> moveToNeighbourRegion(size, regions, cell) }

    private fun moveRandomBorderCell(size: Int, regions: IntArray, solutionCells: Set<Int>): Boolean =
        regions.indices.shuffled(random).any { cell ->
            cell !in solutionCells && moveToNeighbourRegion(size, regions, cell)
        }

    /** Moves [cell] into a random edge-adjacent region, if its own region stays connected. */
    private fun moveToNeighbourRegion(size: Int, regions: IntArray, cell: Int): Boolean {
        val donor = regions[cell]
        val receivers = edgeNeighbours(size, cell).map { regions[it] }.filter { it != donor }.distinct()
        if (receivers.isEmpty() || !staysConnectedWithout(size, regions, donor, cell)) return false
        regions[cell] = receivers.random(random)
        return true
    }

    private fun staysConnectedWithout(size: Int, regions: IntArray, region: Int, removed: Int): Boolean {
        val members = regions.indices.filter { regions[it] == region && it != removed }
        if (members.isEmpty()) return false
        val seen = HashSet<Int>()
        val queue = ArrayDeque(listOf(members.first()))
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            if (!seen.add(cell)) continue
            edgeNeighbours(size, cell).filterTo(queue) { regions[it] == region && it != removed && it !in seen }
        }
        return seen.size == members.size
    }

    private fun edgeNeighbours(size: Int, cell: Int): List<Int> {
        val row = cell / size
        val col = cell % size
        val result = ArrayList<Int>(4)
        if (row > 0) result.add(cell - size)
        if (row < size - 1) result.add(cell + size)
        if (col > 0) result.add(cell - 1)
        if (col < size - 1) result.add(cell + 1)
        return result
    }

    companion object {
        const val DEFAULT_MAX_LAYOUTS = 40
        const val DEFAULT_MAX_REPAIRS = 200
        private const val UNASSIGNED = -1
    }
}
