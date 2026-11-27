package com.payfacto.braingames.feature.sudoku.model

data class SudokuBoard(
    val solution: IntArray = IntArray(81),
    val given: BooleanArray = BooleanArray(81),
    val current: IntArray = IntArray(81),
    val notes: Array<Set<Int>> = Array(81) { emptySet() }
) {
    fun isComplete(): Boolean = current.indices.all { i ->
        current[i] == solution[i]
    }

    /** True when no cell is empty (right or wrong). */
    fun isFilled(): Boolean = current.none { it == 0 }

    /** Indices whose value differs from the solution; empty cells count as wrong. */
    fun wrongCells(): Set<Int> = current.indices.filterTo(LinkedHashSet()) { current[it] != solution[it] }

    /** A copy with one cell's value and notes replaced. */
    fun withCell(index: Int, value: Int, notes: Set<Int>): SudokuBoard {
        val newCurrent = current.copyOf().also { it[index] = value }
        val newNotes = this.notes.copyOf().also { it[index] = notes }
        return copy(current = newCurrent, notes = newNotes)
    }

    /** Snapshot of one cell, for undo. */
    fun moveAt(index: Int): SudokuMove = SudokuMove(index, current[index], notes[index])

    /** The other cells in the same row, column and box as [index]. */
    fun peersOf(index: Int): Set<Int> {
        val row = index / 9
        val col = index % 9
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        return (0 until 9).flatMapTo(LinkedHashSet()) { i ->
            listOf(row * 9 + i, i * 9 + col, (boxRow + i / 3) * 9 + (boxCol + i % 3))
        } - index
    }

    fun conflictsAt(index: Int): Boolean {
        val value = current[index]
        if (value == 0) return false
        val row = index / 9
        val col = index % 9
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (i in 0 until 9) {
            val rowIdx = row * 9 + i
            val colIdx = i * 9 + col
            val boxIdx = (boxRow + i / 3) * 9 + (boxCol + i % 3)
            if (rowIdx != index && current[rowIdx] == value) return true
            if (colIdx != index && current[colIdx] == value) return true
            if (boxIdx != index && current[boxIdx] == value) return true
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SudokuBoard) return false
        return solution.contentEquals(other.solution) &&
                given.contentEquals(other.given) &&
                current.contentEquals(other.current) &&
                notes.contentDeepEquals(other.notes)
    }

    override fun hashCode(): Int {
        var result = solution.contentHashCode()
        result = 31 * result + given.contentHashCode()
        result = 31 * result + current.contentHashCode()
        result = 31 * result + notes.contentDeepHashCode()
        return result
    }
}
