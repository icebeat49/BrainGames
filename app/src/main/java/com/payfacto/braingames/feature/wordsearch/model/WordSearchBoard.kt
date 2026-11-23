package com.payfacto.braingames.feature.wordsearch.model

data class WordSearchBoard(
    val grid: Array<CharArray>,
    val words: List<WordEntry>,
    val theme: String = "",
    val hiddenWord: String = "",
    val hint: String = "",
    val hiddenWordCells: List<Pair<Int, Int>> = emptyList()
) {
    val rows: Int get() = grid.size
    val cols: Int get() = if (grid.isEmpty()) 0 else grid[0].size

    fun isComplete(): Boolean = words.all { it.found }

    /**
     * A puzzle is only valid if every cell belongs to exactly one of:
     * a listed word, or the hidden word. No cell may be both, none may be neither.
     */
    fun verify(): Boolean {
        val wordCells = words.flatMap { entry ->
            entry.word.indices.map { i ->
                Pair(entry.startRow + i * entry.direction.dRow, entry.startCol + i * entry.direction.dCol)
            }
        }.toSet()
        val hiddenCells = hiddenWordCells.toSet()
        val totalCells = rows * cols
        return wordCells.intersect(hiddenCells).isEmpty() &&
            wordCells.size + hiddenCells.size == totalCells &&
            grid.all { row -> row.all { it != ' ' } }
    }

    fun remainingLetterCount(): Int {
        val covered = mutableSetOf<Pair<Int, Int>>()
        for (entry in words) {
            if (entry.found) {
                for (i in entry.word.indices) {
                    covered.add(Pair(
                        entry.startRow + i * entry.direction.dRow,
                        entry.startCol + i * entry.direction.dCol
                    ))
                }
            }
        }
        return rows * cols - covered.size
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WordSearchBoard) return false
        return grid.contentDeepEquals(other.grid) &&
                words == other.words &&
                hiddenWord == other.hiddenWord
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + words.hashCode()
        result = 31 * result + hiddenWord.hashCode()
        return result
    }
}
