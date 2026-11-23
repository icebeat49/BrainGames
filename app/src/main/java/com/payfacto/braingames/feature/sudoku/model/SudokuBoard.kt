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
