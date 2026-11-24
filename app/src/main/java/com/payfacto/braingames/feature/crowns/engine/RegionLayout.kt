package com.payfacto.braingames.feature.crowns.engine

import kotlin.random.Random

/**
 * The generator's mutable region map, which keeps region sizes within [rules]:
 * - every region has at least 2 cells, except [singleCellRegion], which stays exactly 1 cell;
 * - at most [RegionRules.maxTwoCellRegions] regions have exactly 2 cells.
 *
 * Regions grow from the solution cells ([seeds], one per region). Once [isShapeAllowed],
 * [moveToNeighbourRegion] only makes moves that keep it so.
 */
internal class RegionLayout(
    private val size: Int,
    seeds: IntArray,
    private val rules: RegionRules,
    val singleCellRegion: Int?
) {
    private val regions = IntArray(size * size) { UNASSIGNED }
    private val sizes = IntArray(size)
    private val protectedCells: Set<Int> = seeds.toSet()

    init {
        seeds.forEachIndexed { region, cell -> assign(cell, region) }
    }

    fun regions(): IntArray = regions.copyOf()

    fun regionOf(cell: Int): Int = regions[cell]

    fun sizeOf(region: Int): Int = sizes[region]

    val isShapeAllowed: Boolean get() = shapeViolation() == 0

    /**
     * Balanced flood fill: repeatedly grows the smallest region that can still grow by one
     * random free neighbouring cell, so no seed gets boxed in early. The single-cell region
     * never grows. False if free cells are left that no region can reach.
     */
    fun growBalanced(random: Random): Boolean {
        var free = regions.count { it == UNASSIGNED }
        while (free > 0) {
            val frontiers = (0 until size).filter { it != singleCellRegion }
                .associateWith { region -> freeNeighboursOf(region) }
                .filterValues { it.isNotEmpty() }
            if (frontiers.isEmpty()) return false
            val smallest = frontiers.keys.minOf { sizes[it] }
            val region = frontiers.keys.filter { sizes[it] == smallest }.random(random)
            assign(frontiers.getValue(region).random(random), region)
            free--
        }
        return true
    }

    /**
     * Brings undersized regions (or too many 2-cell regions) back within the rules by moving
     * border cells into them from larger neighbours, each move keeping the donor connected.
     * Every move strictly reduces the violation, so this ends. False if no move helps.
     */
    fun fixSmallRegions(random: Random): Boolean {
        while (true) {
            val violation = shapeViolation()
            if (violation == 0) return true
            val (cell, receiver) = movableCells().shuffled(random).firstOrNull { (cell, receiver) ->
                val donor = regions[cell]
                violationAfterMove(donor, receiver) < violation && staysConnectedWithout(donor, cell)
            } ?: return false
            move(cell, receiver)
        }
    }

    /**
     * Moves [cell] into a random edge-adjacent region, if its own region stays connected and
     * the shape stays within the rules. Seed cells never move.
     */
    fun moveToNeighbourRegion(cell: Int, random: Random): Boolean {
        if (cell in protectedCells) return false
        val donor = regions[cell]
        val receivers = edgeNeighbours(cell).map { regions[it] }
            .filter { it != donor && canMove(donor, it) }
            .distinct()
        if (receivers.isEmpty() || !staysConnectedWithout(donor, cell)) return false
        move(cell, receivers.random(random))
        return true
    }

    /** Whether moving one cell from [donor] to [receiver] keeps an allowed shape allowed. */
    private fun canMove(donor: Int, receiver: Int): Boolean =
        donor != singleCellRegion && receiver != singleCellRegion &&
            sizes[donor] - 1 >= MIN_REGION_SIZE &&
            violationAfterMove(donor, receiver) == 0

    // ── Shape ───────────────────────────────────────────────────────────────

    /** 0 when the shape follows the rules; otherwise grows with how far off it is. */
    private fun shapeViolation(): Int = violationOf { sizes[it] }

    private fun violationAfterMove(donor: Int, receiver: Int): Int = violationOf { region ->
        when (region) {
            donor -> sizes[region] - 1
            receiver -> sizes[region] + 1
            else -> sizes[region]
        }
    }

    private inline fun violationOf(sizeOf: (Int) -> Int): Int {
        var missingCells = 0
        var twoCellRegions = 0
        for (region in 0 until size) {
            val regionSize = sizeOf(region)
            if (region == singleCellRegion) {
                missingCells += kotlin.math.abs(regionSize - 1)
                continue
            }
            missingCells += maxOf(0, MIN_REGION_SIZE - regionSize)
            if (regionSize == 2) twoCellRegions++
        }
        val extraTwoCellRegions = maxOf(0, twoCellRegions - rules.maxTwoCellRegions)
        return missingCells * size * size + extraTwoCellRegions
    }

    // ── Cells ───────────────────────────────────────────────────────────────

    /** Non-seed cells next to another region, paired with that region. */
    private fun movableCells(): List<Pair<Int, Int>> = regions.indices
        .filter { it !in protectedCells }
        .flatMap { cell ->
            edgeNeighbours(cell).map { regions[it] }.filter { it != regions[cell] }.distinct().map { cell to it }
        }

    private fun freeNeighboursOf(region: Int): List<Int> = regions.indices
        .filter { regions[it] == UNASSIGNED && edgeNeighbours(it).any { n -> regions[n] == region } }

    private fun assign(cell: Int, region: Int) {
        regions[cell] = region
        sizes[region]++
    }

    private fun move(cell: Int, receiver: Int) {
        sizes[regions[cell]]--
        assign(cell, receiver)
    }

    private fun staysConnectedWithout(region: Int, removed: Int): Boolean {
        val members = regions.indices.filter { regions[it] == region && it != removed }
        if (members.isEmpty()) return false
        val seen = HashSet<Int>()
        val queue = ArrayDeque(listOf(members.first()))
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            if (!seen.add(cell)) continue
            edgeNeighbours(cell).filterTo(queue) { regions[it] == region && it != removed && it !in seen }
        }
        return seen.size == members.size
    }

    private fun edgeNeighbours(cell: Int): List<Int> {
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
        const val MIN_REGION_SIZE = 2
        private const val UNASSIGNED = -1
    }
}
