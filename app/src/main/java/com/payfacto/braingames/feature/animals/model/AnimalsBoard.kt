package com.payfacto.braingames.feature.animals.model

/** What the player has put in a cell. */
enum class CellMark {
    EMPTY,
    /** "No animal here." */
    CROSS,
    /** "Not sure yet": treated as empty by the rules, the solver and hints. */
    QUESTION,
    ANIMAL
}

/**
 * The player's marks, one per cell, row-major. Immutable: every change returns a new
 * board, so a board is its own snapshot for undo.
 */
data class AnimalsBoard(
    val size: Int,
    val marks: List<CellMark>
) {
    init {
        require(marks.size == size * size) { "expected ${size * size} marks, got ${marks.size}" }
    }

    operator fun get(cell: Int): CellMark = marks[cell]

    fun withMark(cell: Int, mark: CellMark): AnimalsBoard = withMarks(listOf(cell), mark)

    fun withMarks(cells: Collection<Int>, mark: CellMark): AnimalsBoard {
        if (cells.all { marks[it] == mark }) return this
        val updated = marks.toMutableList()
        cells.forEach { updated[it] = mark }
        return copy(marks = updated)
    }

    fun cellsWith(mark: CellMark): Set<Int> = marks.indices.filterTo(LinkedHashSet()) { marks[it] == mark }

    val animals: Set<Int> get() = cellsWith(CellMark.ANIMAL)

    companion object {
        fun empty(size: Int) = AnimalsBoard(size, List(size * size) { CellMark.EMPTY })
    }
}
