package com.payfacto.braingames.feature.kakuro.engine

/**
 * Human techniques, easiest first. The grade of a puzzle is the hardest one a step-by-step
 * solve needs when it always uses the easiest technique that still makes progress.
 */
enum class KakuroTechnique {
    /**
     * Each run keeps only digits from sum combinations that contain its placed digits; placed
     * digits leave the rest of the run. Covers unique sums ("3 in two" = 1+2), the last empty
     * cell of a run taking the remainder, and a cell left with one digit.
     */
    SUM_COMBINATIONS,

    /**
     * A digit stays only if the whole run can still be filled with it, given every cell's
     * remaining digits: hidden singles in a run, and combinations ruled out by the cells.
     */
    RUN_PERMUTATIONS,

    /** Trying a digit and following the two techniques above to a contradiction removes it. */
    CONTRADICTION,
}

/** How a logical solve went: the hardest technique used and how many rounds of each. */
data class KakuroGrade(val hardest: KakuroTechnique, val rounds: Map<KakuroTechnique, Int>) {
    fun roundsOf(technique: KakuroTechnique): Int = rounds[technique] ?: 0
}

/**
 * Logical solver and solution counter. Candidates are bitmasks per cell: bit `d - 1` set when
 * digit `d` is still possible.
 */
object KakuroSolver {

    private const val ALL_DIGITS = 0x1FF

    /** The grade, or null when these techniques cannot finish the puzzle (or it has no solution). */
    fun grade(clues: KakuroClues): KakuroGrade? {
        val candidates = initialCandidates(clues)
        val rounds = mutableMapOf<KakuroTechnique, Int>()
        var hardest = KakuroTechnique.SUM_COMBINATIONS
        try {
            while (!isSolved(clues, candidates)) {
                val technique = nextRound(clues, candidates) ?: return null
                rounds[technique] = (rounds[technique] ?: 0) + 1
                if (technique > hardest) hardest = technique
            }
        } catch (ignored: Contradiction) {
            return null
        }
        return KakuroGrade(hardest, rounds)
    }

    /** Up to [limit] solutions, each as digits per cell (0 for blocks). */
    fun solutions(clues: KakuroClues, limit: Int = 2): List<IntArray> {
        val found = mutableListOf<IntArray>()
        search(clues, initialCandidates(clues), limit, found)
        return found
    }

    fun countSolutions(clues: KakuroClues, limit: Int = 2): Int = solutions(clues, limit).size

    // ── Rounds ──────────────────────────────────────────────────────────────

    /** Applies the easiest technique that changes something. Returns it, or null when stuck. */
    private fun nextRound(clues: KakuroClues, candidates: IntArray): KakuroTechnique? = when {
        sumCombinations(clues, candidates) -> KakuroTechnique.SUM_COMBINATIONS
        runPermutations(clues, candidates) -> KakuroTechnique.RUN_PERMUTATIONS
        contradiction(clues, candidates) -> KakuroTechnique.CONTRADICTION
        else -> null
    }

    private fun sumCombinations(clues: KakuroClues, candidates: IntArray): Boolean {
        var changed = false
        for (run in clues.runs) {
            var placed = 0
            for (cell in run.cells) {
                val mask = candidates[cell]
                if (isSingle(mask)) {
                    if (placed and mask != 0) throw DEAD_END
                    placed = placed or mask
                }
            }
            var allowed = 0
            for (combo in KakuroCombinations.of(run.length, run.sum)) {
                if (combo and placed == placed) allowed = allowed or combo
            }
            if (allowed == 0) throw DEAD_END
            for (cell in run.cells) {
                val mask = candidates[cell]
                if (isSingle(mask)) continue
                val narrowed = mask and allowed and placed.inv()
                if (narrowed == 0) throw DEAD_END
                if (narrowed != mask) {
                    candidates[cell] = narrowed
                    changed = true
                }
            }
        }
        return changed
    }

    private fun runPermutations(clues: KakuroClues, candidates: IntArray): Boolean {
        var changed = false
        for (run in clues.runs) {
            val support = RunMatcher.support(run, candidates)
            for ((i, cell) in run.cells.withIndex()) {
                if (support[i] == 0) throw DEAD_END
                if (support[i] != candidates[cell]) {
                    candidates[cell] = support[i]
                    changed = true
                }
            }
        }
        return changed
    }

    /** Removes the first digit, in cell order with fewest candidates first, that leads to a contradiction. */
    private fun contradiction(clues: KakuroClues, candidates: IntArray): Boolean {
        val open = clues.whiteCells.filter { !isSingle(candidates[it]) }.sortedBy { Integer.bitCount(candidates[it]) }
        for (cell in open) {
            var mask = candidates[cell]
            while (mask != 0) {
                val digit = mask and -mask
                mask = mask and (mask - 1)
                val trial = candidates.copyOf()
                trial[cell] = digit
                if (!propagates(clues, trial)) {
                    candidates[cell] = candidates[cell] and digit.inv()
                    if (candidates[cell] == 0) throw DEAD_END
                    return true
                }
            }
        }
        return false
    }

    /** Runs the two run techniques to a fixed point. False on a contradiction. */
    private fun propagates(clues: KakuroClues, candidates: IntArray): Boolean = try {
        while (sumCombinations(clues, candidates) || runPermutations(clues, candidates)) Unit
        true
    } catch (ignored: Contradiction) {
        false
    }

    // ── Search ──────────────────────────────────────────────────────────────

    private fun search(clues: KakuroClues, candidates: IntArray, limit: Int, found: MutableList<IntArray>) {
        if (found.size >= limit || !propagates(clues, candidates)) return
        val open = clues.whiteCells.filter { !isSingle(candidates[it]) }
        if (open.isEmpty()) {
            found += IntArray(clues.cellCount) { cell -> digitOf(candidates[cell]) }
            return
        }
        val cell = open.minBy { Integer.bitCount(candidates[it]) }
        var mask = candidates[cell]
        while (mask != 0 && found.size < limit) {
            val digit = mask and -mask
            mask = mask and (mask - 1)
            val branch = candidates.copyOf()
            branch[cell] = digit
            search(clues, branch, limit, found)
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun initialCandidates(clues: KakuroClues): IntArray =
        IntArray(clues.cellCount).also { candidates -> clues.whiteCells.forEach { candidates[it] = ALL_DIGITS } }

    private fun isSolved(clues: KakuroClues, candidates: IntArray): Boolean =
        clues.whiteCells.all { isSingle(candidates[it]) }

    private fun isSingle(mask: Int): Boolean = mask != 0 && mask and (mask - 1) == 0

    /** The digit of a single-candidate mask, 0 for an empty one. */
    private fun digitOf(mask: Int): Int = if (mask == 0) 0 else Integer.numberOfTrailingZeros(mask) + 1

    /** Thrown inside a solve when some cell or run has no possibility left. No stack trace: it is control flow. */
    private class Contradiction : RuntimeException(null, null, false, false)

    private val DEAD_END = Contradiction()
}

/** The digit sets that fill a run: bitmasks (bit `d - 1` = digit `d`) by length and sum. */
object KakuroCombinations {
    private const val MAX_SUM = 45

    private val table: Array<Array<IntArray>> = Array(10) { length ->
        Array(MAX_SUM + 1) { sum ->
            (0 until 512).filter { Integer.bitCount(it) == length && sumOf(it) == sum }.toIntArray()
        }
    }

    fun of(length: Int, sum: Int): IntArray =
        if (length !in 1..9 || sum !in 0..MAX_SUM) IntArray(0) else table[length][sum]

    private fun sumOf(mask: Int): Int = (0 until 9).filter { mask and (1 shl it) != 0 }.sumOf { it + 1 }
}

/**
 * For one run, which candidates take part in at least one complete fill: a digit per cell,
 * all different, from one sum combination. Dynamic programming over (cell, digits used).
 */
private object RunMatcher {
    private const val MASKS = 512

    fun support(run: KakuroPuzzle.Run, candidates: IntArray): IntArray {
        val n = run.length
        val cellMasks = IntArray(n) { candidates[run.cells[it]] }
        val support = IntArray(n)
        val forward = Array(n + 1) { BooleanArray(MASKS) }
        val backward = Array(n + 1) { BooleanArray(MASKS) }
        for (combo in KakuroCombinations.of(n, run.sum)) {
            if (cellMasks.any { it and combo == 0 }) continue
            forward.forEach { it.fill(false) }
            forward[0][0] = true
            for (i in 0 until n) {
                for (used in 0 until MASKS) {
                    if (!forward[i][used]) continue
                    var free = cellMasks[i] and combo and used.inv()
                    while (free != 0) {
                        val digit = free and -free
                        free = free and (free - 1)
                        forward[i + 1][used or digit] = true
                    }
                }
            }
            if (!forward[n][combo]) continue
            backward.forEach { it.fill(false) }
            backward[n][combo] = true
            for (i in n - 1 downTo 0) {
                for (used in 0 until MASKS) {
                    if (!forward[i][used]) continue
                    var free = cellMasks[i] and combo and used.inv()
                    while (free != 0) {
                        val digit = free and -free
                        free = free and (free - 1)
                        if (backward[i + 1][used or digit]) {
                            backward[i][used] = true
                            support[i] = support[i] or digit
                        }
                    }
                }
            }
        }
        return support
    }
}
