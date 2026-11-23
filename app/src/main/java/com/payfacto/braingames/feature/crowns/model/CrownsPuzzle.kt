package com.payfacto.braingames.feature.crowns.model

import kotlin.math.abs

/**
 * An N×N grid split into N regions. The player hides one crown per row, per column and
 * per region, and no two crowns may touch, not even diagonally.
 *
 * Cells are numbered row-major: `cell = row * size + column`.
 *
 * Immutable: the arrays passed in are copied, and the accessors return copies.
 *
 * @param regions region id (0 until size) of each cell, `size * size` entries.
 * @param solution column of the crown in each row, `size` entries.
 */
class CrownsPuzzle(
    val size: Int,
    regions: IntArray,
    solution: IntArray
) {
    private val regionIds: IntArray = regions.copyOf()
    private val solutionColumns: IntArray = solution.copyOf()

    init {
        require(size >= MIN_SIZE) { "size must be at least $MIN_SIZE, was $size" }
        require(regionIds.size == size * size) { "expected ${size * size} region ids, got ${regionIds.size}" }
        require(regionIds.all { it in 0 until size }) { "region ids must be in 0 until $size" }
        require(solutionColumns.size == size) { "expected $size solution columns, got ${solutionColumns.size}" }
        require(solutionColumns.all { it in 0 until size }) { "solution columns must be in 0 until $size" }
    }

    val cellCount: Int get() = size * size

    /** Region id of each cell (a copy). */
    val regions: IntArray get() = regionIds.copyOf()

    /** Column of the crown in each row (a copy). */
    val solution: IntArray get() = solutionColumns.copyOf()

    /** The cells holding a crown in the solution. */
    val solutionCells: Set<Int> by lazy {
        solutionColumns.withIndex().mapTo(LinkedHashSet()) { (row, col) -> cellAt(row, col) }
    }

    fun cellAt(row: Int, column: Int): Int = row * size + column
    fun rowOf(cell: Int): Int = cell / size
    fun columnOf(cell: Int): Int = cell % size
    fun regionOf(cell: Int): Int = regionIds[cell]

    /** Same cell excluded. True for the 8 surrounding cells. */
    fun touches(a: Int, b: Int): Boolean =
        a != b && abs(rowOf(a) - rowOf(b)) <= 1 && abs(columnOf(a) - columnOf(b)) <= 1

    /** True when crowns on [a] and [b] could not both stand: same row, column or region, or touching. */
    fun excludes(a: Int, b: Int): Boolean =
        a != b && (rowOf(a) == rowOf(b) || columnOf(a) == columnOf(b) ||
            regionOf(a) == regionOf(b) || touches(a, b))

    val rowCells: List<List<Int>> by lazy { List(size) { r -> List(size) { c -> cellAt(r, c) } } }
    val columnCells: List<List<Int>> by lazy { List(size) { c -> List(size) { r -> cellAt(r, c) } } }
    val regionCells: List<List<Int>> by lazy {
        List(size) { region -> (0 until cellCount).filter { regionIds[it] == region } }
    }

    // Note: inside buildList { } `size` would mean the list being built, hence the local `n`.

    /** Up to 8 surrounding cells. */
    fun neighboursOf(cell: Int): List<Int> = neighbours[cell]

    /** Up to 4 cells sharing an edge: what makes a region connected. */
    fun orthogonalNeighboursOf(cell: Int): List<Int> = orthogonalNeighbours[cell]

    private val neighbours: List<List<Int>> by lazy {
        val n = size
        List(cellCount) { cell ->
            val row = rowOf(cell)
            val col = columnOf(cell)
            buildList {
                for (dr in -1..1) for (dc in -1..1) {
                    val r = row + dr
                    val c = col + dc
                    if ((dr != 0 || dc != 0) && r in 0 until n && c in 0 until n) add(r * n + c)
                }
            }
        }
    }

    private val orthogonalNeighbours: List<List<Int>> by lazy {
        val n = size
        List(cellCount) { cell ->
            val row = rowOf(cell)
            val col = columnOf(cell)
            buildList {
                if (row > 0) add(cell - n)
                if (row < n - 1) add(cell + n)
                if (col > 0) add(cell - 1)
                if (col < n - 1) add(cell + 1)
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        other is CrownsPuzzle && size == other.size &&
            regionIds.contentEquals(other.regionIds) && solutionColumns.contentEquals(other.solutionColumns)

    override fun hashCode(): Int =
        31 * (31 * size + regionIds.contentHashCode()) + solutionColumns.contentHashCode()

    override fun toString(): String = buildString {
        append("CrownsPuzzle(size=$size, solution=${solutionColumns.joinToString("")})\n")
        rowCells.forEach { row -> append(row.joinToString("") { ('A' + regionIds[it]).toString() }).append('\n') }
    }

    companion object {
        /** Below 4 no grid can hold non-touching crowns in every row and column. */
        const val MIN_SIZE = 4
    }
}
