package com.payfacto.braingames.core.model

/** The games, with the levels each one offers, easiest first. */
enum class GameType(val levels: List<Difficulty> = Difficulty.STANDARD) {
    SUDOKU,
    KAKURO,
    WORD_SEARCH,
    CROWNS(Difficulty.STANDARD + Difficulty.MASTER)
}
