package com.payfacto.braingames.feature.wordsearch.model

data class WordEntry(
    val word: String,
    val startRow: Int,
    val startCol: Int,
    val direction: Direction,
    val found: Boolean = false
) {
    /** The (row, col) of each letter, first to last. */
    fun cells(): List<Pair<Int, Int>> =
        word.indices.map { i -> Pair(startRow + i * direction.dRow, startCol + i * direction.dCol) }
}

enum class Direction(val dRow: Int, val dCol: Int) {
    RIGHT(0, 1),
    LEFT(0, -1),
    DOWN(1, 0),
    UP(-1, 0),
    DOWN_RIGHT(1, 1),
    DOWN_LEFT(1, -1),
    UP_RIGHT(-1, 1),
    UP_LEFT(-1, -1)
}
