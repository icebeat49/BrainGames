package com.payfacto.braingames.feature.wordsearch.model

data class WordEntry(
    val word: String,
    val startRow: Int,
    val startCol: Int,
    val direction: Direction,
    val found: Boolean = false
)

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
