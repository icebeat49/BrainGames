package com.payfacto.braingames.testutil

import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell
import com.payfacto.braingames.feature.sudoku.model.SudokuBoard

object TestBoards {

    /** Cells left for the player in [sudoku]; every other cell is a given. */
    val SUDOKU_OPEN_CELLS = 0..4

    /** A valid solved grid (shifted-row pattern) with [SUDOKU_OPEN_CELLS] blanked out. */
    fun sudoku(): SudokuBoard {
        val solution = IntArray(81) { i ->
            val row = i / 9
            val col = i % 9
            (row * 3 + row / 3 + col) % 9 + 1
        }
        val given = BooleanArray(81) { it !in SUDOKU_OPEN_CELLS }
        val current = IntArray(81) { if (given[it]) solution[it] else 0 }
        return SudokuBoard(solution = solution, given = given, current = current)
    }

    /**
     * 3×3 Kakuro with a unique solution:
     * ```
     *  ■   ↓4  ↓3
     *  →3  1   2
     *  →4  3   1
     * ```
     */
    fun kakuro(): KakuroBoard = KakuroBoard(
        rows = 3,
        cols = 3,
        cells = listOf(
            listOf(KakuroCell.Black, KakuroCell.Clue(downSum = 4, acrossSum = 0), KakuroCell.Clue(downSum = 3, acrossSum = 0)),
            listOf(KakuroCell.Clue(downSum = 0, acrossSum = 3), KakuroCell.Input(solution = 1), KakuroCell.Input(solution = 2)),
            listOf(KakuroCell.Clue(downSum = 0, acrossSum = 4), KakuroCell.Input(solution = 3), KakuroCell.Input(solution = 1)),
        )
    )
}
