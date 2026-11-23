package com.payfacto.braingames.feature.animals.engine

import com.payfacto.braingames.feature.animals.model.AnimalsBoard
import com.payfacto.braingames.feature.animals.model.AnimalsPuzzle
import com.payfacto.braingames.feature.animals.model.CellMark

/**
 * The solver's working state: where animals are, and where one could still go.
 * Placing an animal removes every candidate it excludes (row, column, region and the
 * 8 surrounding cells).
 */
class CandidateGrid private constructor(
    val puzzle: AnimalsPuzzle,
    private val candidates: BooleanArray,
    private val animals: BooleanArray
) {
    var animalCount: Int = animals.count { it }
        private set

    fun isCandidate(cell: Int): Boolean = candidates[cell]
    fun hasAnimal(cell: Int): Boolean = animals[cell]

    fun cellsOf(area: Area): List<Int> = when (area.type) {
        AreaType.ROW -> puzzle.rowCells[area.index]
        AreaType.COLUMN -> puzzle.columnCells[area.index]
        AreaType.REGION -> puzzle.regionCells[area.index]
    }

    fun candidatesOf(area: Area): List<Int> = cellsOf(area).filter { candidates[it] }
    fun hasAnimalIn(area: Area): Boolean = cellsOf(area).any { animals[it] }

    /** Every row, column and region. */
    val areas: List<Area> by lazy {
        AreaType.entries.flatMap { type -> List(puzzle.size) { Area(type, it) } }
    }

    val isSolved: Boolean get() = animalCount == puzzle.size

    /** Some row, column or region has no animal and nowhere left to put one. */
    val isContradictory: Boolean
        get() = areas.any { area -> !hasAnimalIn(area) && candidatesOf(area).isEmpty() }

    fun place(cell: Int) {
        if (animals[cell]) return
        animals[cell] = true
        animalCount++
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

    fun copy() = CandidateGrid(puzzle, candidates.copyOf(), animals.copyOf())

    companion object {
        fun fresh(puzzle: AnimalsPuzzle) =
            CandidateGrid(puzzle, BooleanArray(puzzle.cellCount) { true }, BooleanArray(puzzle.cellCount))

        /**
         * What the player's marks tell the solver: crosses remove candidates and animals
         * are placed. Question marks count as empty. Assumes the marks are correct;
         * check [AnimalsValidator.wrongMarks] first.
         */
        fun fromBoard(puzzle: AnimalsPuzzle, board: AnimalsBoard): CandidateGrid {
            val grid = fresh(puzzle)
            board.cellsWith(CellMark.CROSS).forEach(grid::eliminate)
            board.animals.forEach(grid::place)
            return grid
        }
    }
}
