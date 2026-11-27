package com.payfacto.braingames.feature.kakuro.engine

import kotlin.random.Random

/** Shape of the grids generated for one difficulty. */
data class KakuroGenerationRules(
    /** Grid side, clue row and column included. */
    val size: Int,
    /** Share of white cells among the cells below and right of the clue row and column. */
    val whiteShare: ClosedFloatingPointRange<Double>,
    /** Longest run allowed. */
    val maxRunLength: Int,
)

/**
 * Builds Kakuro puzzles with exactly one solution:
 * 1. a layout with 180° rotational symmetry, every run 2 to [KakuroGenerationRules.maxRunLength]
 *    cells long and all white cells connected;
 * 2. random digits, all different within each run;
 * 3. while a second solution exists, the cells where the two solutions differ are cleared and
 *    refilled at random (which changes their clues), up to [MAX_REPAIRS] times.
 *
 * Grading is left to the caller ([KakuroSolver.grade]).
 */
class KakuroGenerator(private val random: Random = Random.Default) {
    /** One unique puzzle, or null when this attempt gave up (call again). */
    fun generate(rules: KakuroGenerationRules): KakuroPuzzle? {
        val white = layout(rules) ?: return null
        val shape = Shape(rules.size, white)
        val digits = IntArray(rules.size * rules.size)
        if (!fillEmpty(shape, digits)) return null
        return makeUnique(shape, digits)
    }

    // ── 1. Layout ───────────────────────────────────────────────────────────

    private fun layout(rules: KakuroGenerationRules): BooleanArray? {
        val n = rules.size
        val white = BooleanArray(n * n) { cell -> cell / n > 0 && cell % n > 0 }
        val interior = (n - 1) * (n - 1)
        val target = (random.nextDouble(rules.whiteShare.start, rules.whiteShare.endInclusive) * interior).toInt()

        // Block cells in random order, skipping any block that would leave a 1-cell run.
        for (cell in white.indices.filter { white[it] }.shuffled(random)) {
            if (white.count { it } <= target) break
            if (white[cell]) tryBlock(white, n, cell)
        }
        if (!splitLongRuns(white, n, rules.maxRunLength)) return null

        val share = white.count { it }.toDouble() / interior
        if (share < rules.whiteShare.start - SHARE_SLACK || !isConnected(white, n)) return null
        return white
    }

    /** Cuts every run longer than [maxRun] where a block leaves no 1-cell run. False if one cannot be cut. */
    private fun splitLongRuns(white: BooleanArray, n: Int, maxRun: Int): Boolean {
        while (true) {
            val long = lines(n).flatMap { runsIn(it, white) }.firstOrNull { it.size > maxRun } ?: return true
            if (long.shuffled(random).none { tryBlock(white, n, it) }) return false
        }
    }

    /** Blocks [cell] and its mirror unless that leaves a 1-cell run. Returns whether it did. */
    private fun tryBlock(white: BooleanArray, n: Int, cell: Int): Boolean {
        val mirror = (n - cell / n) * n + (n - cell % n)
        val trial = white.copyOf()
        trial[cell] = false
        trial[mirror] = false
        val touched = setOf(cell / n, mirror / n).map { r -> List(n) { c -> r * n + c } } +
            setOf(cell % n, mirror % n).map { c -> List(n) { r -> r * n + c } }
        if (touched.any { line -> runsIn(line, trial).any { it.size == 1 } }) return false
        white[cell] = false
        white[mirror] = false
        return true
    }

    private fun isConnected(white: BooleanArray, n: Int): Boolean {
        val start = white.indexOfFirst { it }
        if (start < 0) return false
        val seen = BooleanArray(white.size)
        val stack = ArrayDeque(listOf(start))
        seen[start] = true
        var count = 0
        while (stack.isNotEmpty()) {
            val cell = stack.removeLast()
            count++
            val r = cell / n
            val c = cell % n
            for ((dr, dc) in NEIGHBOURS) {
                val nr = r + dr
                val nc = c + dc
                if (nr !in 0 until n || nc !in 0 until n) continue
                val next = nr * n + nc
                if (white[next] && !seen[next]) {
                    seen[next] = true
                    stack.addLast(next)
                }
            }
        }
        return count == white.count { it }
    }

    // ── 2. Fill ─────────────────────────────────────────────────────────────

    /**
     * Gives every white cell still at 0 a random digit, different from the rest of its runs,
     * most constrained cell first. False if no such fill exists within [MAX_FILL_STEPS].
     */
    private fun fillEmpty(shape: Shape, digits: IntArray): Boolean {
        var steps = 0
        fun available(cell: Int): Int {
            var used = 0
            shape.peers[cell].forEach { if (digits[it] != 0) used = used or (1 shl (digits[it] - 1)) }
            return ALL_DIGITS and used.inv()
        }
        fun place(): Boolean {
            if (++steps > MAX_FILL_STEPS) return false
            val open = shape.whites.filter { digits[it] == 0 }
            if (open.isEmpty()) return true
            val cell = open.minBy { Integer.bitCount(available(it)) }
            val options = available(cell)
            for (digit in (1..9).filter { options and (1 shl (it - 1)) != 0 }.shuffled(random)) {
                digits[cell] = digit
                if (place()) return true
            }
            digits[cell] = 0
            return false
        }
        return place()
    }

    // ── 3. Uniqueness ───────────────────────────────────────────────────────

    private fun makeUnique(shape: Shape, digits: IntArray): KakuroPuzzle? {
        repeat(MAX_REPAIRS) {
            val puzzle = KakuroPuzzle(shape.size, digits)
            val solutions = KakuroSolver.solutions(puzzle.clues, limit = 2)
            val other = solutions.firstOrNull { !it.contentEquals(digits) } ?: return puzzle
            val differing = shape.whites.filter { other[it] != digits[it] }
            if (!refill(shape, digits, differing)) return null
        }
        return null
    }

    /** New random digits for [cells]; if they cannot be refilled alone, their run partners are refilled too. */
    private fun refill(shape: Shape, digits: IntArray, cells: List<Int>): Boolean {
        val saved = digits.copyOf()
        cells.forEach { digits[it] = 0 }
        if (fillEmpty(shape, digits)) return true
        saved.copyInto(digits)
        (cells + cells.flatMap { shape.peers[it].toList() }).forEach { digits[it] = 0 }
        return fillEmpty(shape, digits)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** White cells and, for each, the other cells of its two runs. */
    private class Shape(val size: Int, white: BooleanArray) {
        val whites: List<Int> = white.indices.filter { white[it] }
        val peers: Array<IntArray> = Array(size * size) { IntArray(0) }.also { peers ->
            for (line in lines(size)) {
                for (run in runsIn(line, white)) {
                    run.forEach { cell -> peers[cell] = peers[cell] + run.filter { it != cell } }
                }
            }
        }
    }

    private companion object {
        const val ALL_DIGITS = 0x1FF
        const val MAX_FILL_STEPS = 20_000
        const val MAX_REPAIRS = 60
        /** A layout may end a little below its white share. */
        const val SHARE_SLACK = 0.08
        val NEIGHBOURS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

        /** Every row and every column, as cell lists. */
        fun lines(n: Int): List<List<Int>> =
            List(n) { r -> List(n) { c -> r * n + c } } + List(n) { c -> List(n) { r -> r * n + c } }

        fun runsIn(line: List<Int>, white: BooleanArray): List<List<Int>> {
            val runs = mutableListOf<List<Int>>()
            var current = mutableListOf<Int>()
            for (cell in line) {
                if (white[cell]) {
                    current += cell
                } else if (current.isNotEmpty()) {
                    runs += current
                    current = mutableListOf()
                }
            }
            if (current.isNotEmpty()) runs += current
            return runs
        }
    }
}
