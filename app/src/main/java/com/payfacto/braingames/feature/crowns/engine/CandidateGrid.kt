package com.payfacto.braingames.feature.crowns.engine

import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import com.payfacto.braingames.feature.crowns.model.CellMark

/**
 * The solver's working state: where crowns are, and where one could still go.
 * Placing a crown removes every candidate it excludes (row, column, region and the
 * 8 surrounding cells).
 */
class CandidateGrid private constructor(
    val puzzle: CrownsPuzzle,
    private val candidates: BooleanArray,
    private val crowns: BooleanArray
) {
    var crownCount: Int = crowns.count { it }
        private set

    fun isCandidate(cell: Int): Boolean = candidates[cell]
    fun hasCrown(cell: Int): Boolean = crowns[cell]

    fun cellsOf(area: Area): List<Int> = puzzle.cellsOf(area)

    fun candidatesOf(area: Area): List<Int> = cellsOf(area).filter { candidates[it] }
    fun hasCrownIn(area: Area): Boolean = cellsOf(area).any { crowns[it] }

    /** Every row, column and region. */
    val areas: List<Area> by lazy {
        AreaType.entries.flatMap { type -> List(puzzle.size) { Area(type, it) } }
    }

    val isSolved: Boolean get() = crownCount == puzzle.size

    /** Some row, column or region has no crown and nowhere left to put one. */
    val isContradictory: Boolean
        get() = areas.any { area -> !hasCrownIn(area) && candidatesOf(area).isEmpty() }

    fun place(cell: Int) {
        if (crowns[cell]) return
        crowns[cell] = true
        crownCount++
        candidates[cell] = false
        for (other in candidates.indices) {
            if (candidates[other] && puzzle.excludes(cell, other)) candidates[other] = false
        }
    }

    fun eliminate(cell: Int) {
        candidates[cell] = false
    }

    fun apply(step: SolverStep) {
        step.eliminations.forEach(::eliminate)
        step.placements.forEach(::place)
    }

    fun copy() = CandidateGrid(puzzle, candidates.copyOf(), crowns.copyOf())

    companion object {
        fun fresh(puzzle: CrownsPuzzle) =
            CandidateGrid(puzzle, BooleanArray(puzzle.cellCount) { true }, BooleanArray(puzzle.cellCount))

        /**
         * What the player's marks tell the solver: crosses remove candidates and crowns
         * are placed. Question marks count as empty. Assumes the marks are correct;
         * check [CrownsValidator.wrongMarks] first.
         */
        fun fromBoard(puzzle: CrownsPuzzle, board: CrownsBoard): CandidateGrid {
            val grid = fresh(puzzle)
            board.cellsWith(CellMark.CROSS).forEach(grid::eliminate)
            board.crowns.forEach(grid::place)
            return grid
        }
    }
}
