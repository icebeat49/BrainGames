package com.payfacto.braingames.feature.crowns.model

/** What the player has put in a cell. */
enum class CellMark {
    EMPTY,
    /** "No crown here." */
    CROSS,
    /** "Not sure yet": treated as empty by the rules, the solver and hints. */
    QUESTION,
    CROWN
}

/**
 * The player's marks, one per cell, row-major. Immutable: every change returns a new
 * board, so a board is its own snapshot for undo.
 */
data class CrownsBoard(
    val size: Int,
    val marks: List<CellMark>
) {
    init {
        require(marks.size == size * size) { "expected ${size * size} marks, got ${marks.size}" }
    }

    operator fun get(cell: Int): CellMark = marks[cell]

    fun withMark(cell: Int, mark: CellMark): CrownsBoard = withMarks(listOf(cell), mark)

    fun withMarks(cells: Collection<Int>, mark: CellMark): CrownsBoard {
        if (cells.all { marks[it] == mark }) return this
        val updated = marks.toMutableList()
        cells.forEach { updated[it] = mark }
        return copy(marks = updated)
    }

    fun cellsWith(mark: CellMark): Set<Int> = marks.indices.filterTo(LinkedHashSet()) { marks[it] == mark }

    val crowns: Set<Int> get() = cellsWith(CellMark.CROWN)

    companion object {
        fun empty(size: Int) = CrownsBoard(size, List(size * size) { CellMark.EMPTY })
    }
}
