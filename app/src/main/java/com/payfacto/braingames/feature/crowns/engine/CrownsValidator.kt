package com.payfacto.braingames.feature.crowns.engine

import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import com.payfacto.braingames.feature.crowns.model.CellMark

enum class Rule {
    SAME_ROW,
    SAME_COLUMN,
    SAME_REGION,
    TOUCHING
}

/** Two crowns breaking [rule]. A pair breaking several rules is reported once per rule. */
data class Conflict(val rule: Rule, val first: Int, val second: Int)

/** What can be wrong with a puzzle itself (as opposed to a player's board). */
enum class PuzzleProblem {
    /** Not exactly `size` regions, or a region id with no cells. */
    WRONG_REGION_COUNT,
    /** A region whose cells are not all joined edge to edge. */
    DISCONNECTED_REGION,
    /** The stored solution breaks a rule. */
    INVALID_SOLUTION
}

object CrownsValidator {

    /** Every rule broken by the crowns on [board]. Crosses and question marks are ignored. */
    fun conflicts(puzzle: CrownsPuzzle, board: CrownsBoard): List<Conflict> =
        conflicts(puzzle, board.crowns.sorted())

    /** N crowns and no rule broken. One per row, column and region follows from that. */
    fun isSolved(puzzle: CrownsPuzzle, board: CrownsBoard): Boolean {
        val crowns = board.crowns
        return crowns.size == puzzle.size && conflicts(puzzle, crowns.sorted()).isEmpty()
    }

    /** Crowns placed where the solution has none, then crosses on solution cells. */
    fun wrongMarks(puzzle: CrownsPuzzle, board: CrownsBoard): List<Int> {
        val solution = puzzle.solutionCells
        val wrongCrowns = board.crowns.filter { it !in solution }
        val wrongCrosses = board.cellsWith(CellMark.CROSS).filter { it in solution }
        return wrongCrowns + wrongCrosses
    }

    fun puzzleProblems(puzzle: CrownsPuzzle): Set<PuzzleProblem> = buildSet {
        if (puzzle.regionCells.any { it.isEmpty() }) add(PuzzleProblem.WRONG_REGION_COUNT)
        if (puzzle.regionCells.any { it.isNotEmpty() && !isConnected(puzzle, it) }) {
            add(PuzzleProblem.DISCONNECTED_REGION)
        }
        val solution = puzzle.solutionCells.sorted()
        val oneCrownPerRegion = solution.map(puzzle::regionOf).toSet().size == puzzle.size
        if (!oneCrownPerRegion || conflicts(puzzle, solution).isNotEmpty()) add(PuzzleProblem.INVALID_SOLUTION)
    }

    /** True when every cell of [cells] can be reached from the first through edge neighbours in [cells]. */
    internal fun isConnected(puzzle: CrownsPuzzle, cells: Collection<Int>): Boolean {
        if (cells.isEmpty()) return true
        val members = cells.toHashSet()
        val seen = HashSet<Int>()
        val queue = ArrayDeque(listOf(cells.first()))
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            if (!seen.add(cell)) continue
            puzzle.orthogonalNeighboursOf(cell).filterTo(queue) { it in members && it !in seen }
        }
        return seen.size == members.size
    }

    private fun conflicts(puzzle: CrownsPuzzle, crowns: List<Int>): List<Conflict> = buildList {
        for (i in crowns.indices) for (j in i + 1 until crowns.size) {
            val a = crowns[i]
            val b = crowns[j]
            if (puzzle.rowOf(a) == puzzle.rowOf(b)) add(Conflict(Rule.SAME_ROW, a, b))
            if (puzzle.columnOf(a) == puzzle.columnOf(b)) add(Conflict(Rule.SAME_COLUMN, a, b))
            if (puzzle.regionOf(a) == puzzle.regionOf(b)) add(Conflict(Rule.SAME_REGION, a, b))
            if (puzzle.touches(a, b)) add(Conflict(Rule.TOUCHING, a, b))
        }
    }
}
