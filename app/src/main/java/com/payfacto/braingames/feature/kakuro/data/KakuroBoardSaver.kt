package com.payfacto.braingames.feature.kakuro.data

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell

/**
 * Stores the in-progress board in a [SavedStateHandle] as a flat IntArray:
 * `[rows, cols]` followed by [INTS_PER_CELL] ints per cell (row-major):
 * `type, downSum, acrossSum, solution, current`.
 */
internal object KakuroBoardSaver {
    private const val KEY_BOARD = "kakuro_board"
    private const val HEADER_SIZE = 2
    private const val INTS_PER_CELL = 5

    private const val TYPE_BLACK = 0
    private const val TYPE_CLUE = 1
    private const val TYPE_INPUT = 2

    fun save(handle: SavedStateHandle, board: KakuroBoard) {
        val data = IntArray(HEADER_SIZE + board.rows * board.cols * INTS_PER_CELL)
        data[0] = board.rows
        data[1] = board.cols
        board.cells.flatten().forEachIndexed { i, cell ->
            val base = HEADER_SIZE + i * INTS_PER_CELL
            when (cell) {
                is KakuroCell.Black -> data[base] = TYPE_BLACK
                is KakuroCell.Clue -> {
                    data[base] = TYPE_CLUE
                    data[base + 1] = cell.downSum
                    data[base + 2] = cell.acrossSum
                }
                is KakuroCell.Input -> {
                    data[base] = TYPE_INPUT
                    data[base + 3] = cell.solution
                    data[base + 4] = cell.current
                }
            }
        }
        handle[KEY_BOARD] = data
    }

    fun restore(handle: SavedStateHandle): KakuroBoard? {
        val data = handle.get<IntArray>(KEY_BOARD) ?: return null
        val rows = data[0]
        val cols = data[1]
        val cells = List(rows) { r ->
            List(cols) { c ->
                val base = HEADER_SIZE + (r * cols + c) * INTS_PER_CELL
                when (data[base]) {
                    TYPE_CLUE -> KakuroCell.Clue(downSum = data[base + 1], acrossSum = data[base + 2])
                    TYPE_INPUT -> KakuroCell.Input(solution = data[base + 3], current = data[base + 4])
                    else -> KakuroCell.Black
                }
            }
        }
        return KakuroBoard(rows = rows, cols = cols, cells = cells)
    }

    fun clear(handle: SavedStateHandle) {
        handle.remove<IntArray>(KEY_BOARD)
    }
}
