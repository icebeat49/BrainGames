package com.payfacto.braingames.feature.kakuro.engine

import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell

/**
 * A square Kakuro grid given by its solution: one digit 1–9 per white cell, [BLOCK] for the
 * others. Row 0 and column 0 are always blocks. The clues are not stored: each run's clue is
 * the sum of its solution digits.
 *
 * Cells are numbered row-major: `cell = row * size + column`.
 */
class KakuroPuzzle(val size: Int, solution: IntArray) {

    private val digits: IntArray = solution.copyOf()

    init {
        require(size >= MIN_SIZE) { "size must be at least $MIN_SIZE, was $size" }
        require(digits.size == size * size) { "expected ${size * size} cells, got ${digits.size}" }
        require(digits.all { it == BLOCK || it in 1..9 }) { "cells must be $BLOCK or 1..9" }
        require((0 until size).all { digits[it] == BLOCK && digits[it * size] == BLOCK }) {
            "row 0 and column 0 must be blocks"
        }
    }

    val cellCount: Int get() = size * size

    /** Solution digit of each cell, [BLOCK] for blocks (a copy). */
    val solution: IntArray get() = digits.copyOf()

    fun isWhite(cell: Int): Boolean = digits[cell] != BLOCK
    fun digitAt(cell: Int): Int = digits[cell]

    val whiteCells: List<Int> by lazy { (0 until cellCount).filter(::isWhite) }

    /** Every across run, then every down run, each with its clue. */
    val runs: List<Run> by lazy { acrossRuns() + downRuns() }

    /** The clues alone, as the player sees them. */
    val clues: KakuroClues by lazy { KakuroClues(cellCount, whiteCells, runs) }

    /** The board the game plays on: clue cells where a run starts, empty inputs elsewhere. */
    fun toBoard(): KakuroBoard {
        val acrossClue = runs.filter { it.direction == Direction.ACROSS }.associate { it.clueCell to it.sum }
        val downClue = runs.filter { it.direction == Direction.DOWN }.associate { it.clueCell to it.sum }
        val cells = List(size) { r ->
            List(size) { c ->
                val cell = r * size + c
                when {
                    isWhite(cell) -> KakuroCell.Input(solution = digits[cell])
                    cell in acrossClue || cell in downClue ->
                        KakuroCell.Clue(downSum = downClue[cell] ?: 0, acrossSum = acrossClue[cell] ?: 0)
                    else -> KakuroCell.Black
                }
            }
        }
        return KakuroBoard(rows = size, cols = size, cells = cells)
    }

    // Inside buildList { } `size` would mean the list being built, hence the local `n`.

    private fun acrossRuns(): List<Run> {
        val n = size
        return buildList { for (r in 0 until n) addRuns(this, Direction.ACROSS, List(n) { c -> r * n + c }) }
    }

    private fun downRuns(): List<Run> {
        val n = size
        return buildList { for (c in 0 until n) addRuns(this, Direction.DOWN, List(n) { r -> r * n + c }) }
    }

    /** Splits one line of cells into runs of consecutive white cells, each clued by the block before it. */
    private fun addRuns(into: MutableList<Run>, direction: Direction, line: List<Int>) {
        var start = 0
        while (start < line.size) {
            if (isWhite(line[start])) {
                var end = start
                while (end < line.size && isWhite(line[end])) end++
                val cells = line.subList(start, end).toIntArray()
                into += Run(direction, clueCell = line[start - 1], cells = cells, sum = cells.sumOf { digits[it] })
                start = end
            } else {
                start++
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        other is KakuroPuzzle && size == other.size && digits.contentEquals(other.digits)

    override fun hashCode(): Int = 31 * size + digits.contentHashCode()

    enum class Direction { ACROSS, DOWN }

    /** Consecutive white cells in a row or column whose digits are all different and add up to [sum]. */
    class Run(val direction: Direction, val clueCell: Int, val cells: IntArray, val sum: Int) {
        val length: Int get() = cells.size
    }

    companion object {
        const val BLOCK = 0
        const val MIN_SIZE = 3
    }
}

/** What the solver knows: which cells are white and the runs with their sums. */
class KakuroClues(val cellCount: Int, val whiteCells: List<Int>, val runs: List<KakuroPuzzle.Run>) {
    /** For each cell, the indices of its runs in [runs] (empty for blocks). */
    val runsOfCell: Array<IntArray> = Array(cellCount) { IntArray(0) }.also { table ->
        val lists = Array(cellCount) { mutableListOf<Int>() }
        runs.forEachIndexed { index, run -> run.cells.forEach { lists[it] += index } }
        lists.forEachIndexed { cell, list -> table[cell] = list.toIntArray() }
    }
}
