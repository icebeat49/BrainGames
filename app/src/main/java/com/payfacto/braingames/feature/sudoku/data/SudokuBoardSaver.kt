package com.payfacto.braingames.feature.sudoku.data

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.feature.sudoku.model.SudokuBoard
import com.payfacto.braingames.feature.sudoku.model.SudokuMove

/**
 * Stores the in-progress board and its undo history in a [SavedStateHandle] using Bundle-friendly types,
 * so a ViewModel recreated after process death resumes the same puzzle.
 * Notes are packed as one bitmask per cell (bit n set = note n).
 */
internal object SudokuBoardSaver {
    private const val KEY_SOLUTION = "sudoku_solution"
    private const val KEY_GIVEN = "sudoku_given"
    private const val KEY_CURRENT = "sudoku_current"
    private const val KEY_NOTES = "sudoku_notes"
    private const val KEY_UNDO = "sudoku_undo"
    private const val INTS_PER_MOVE = 3

    fun save(handle: SavedStateHandle, board: SudokuBoard) {
        handle[KEY_SOLUTION] = board.solution.copyOf()
        handle[KEY_GIVEN] = board.given.copyOf()
        handle[KEY_CURRENT] = board.current.copyOf()
        handle[KEY_NOTES] = IntArray(board.notes.size) { i -> board.notes[i].toBitmask() }
    }

    fun restore(handle: SavedStateHandle): SudokuBoard? {
        val solution = handle.get<IntArray>(KEY_SOLUTION) ?: return null
        val given = handle.get<BooleanArray>(KEY_GIVEN) ?: return null
        val current = handle.get<IntArray>(KEY_CURRENT) ?: return null
        val notes = handle.get<IntArray>(KEY_NOTES) ?: return null
        return SudokuBoard(
            solution = solution.copyOf(),
            given = given.copyOf(),
            current = current.copyOf(),
            notes = Array(notes.size) { i -> notes[i].toNoteSet() }
        )
    }

    /** Undo history, oldest first, as `index, value, notesBitmask` triples. */
    fun saveUndo(handle: SavedStateHandle, moves: List<SudokuMove>) {
        handle[KEY_UNDO] = IntArray(moves.size * INTS_PER_MOVE).also { data ->
            moves.forEachIndexed { i, move ->
                val base = i * INTS_PER_MOVE
                data[base] = move.index
                data[base + 1] = move.value
                data[base + 2] = move.notes.toBitmask()
            }
        }
    }

    fun restoreUndo(handle: SavedStateHandle): List<SudokuMove> {
        val data = handle.get<IntArray>(KEY_UNDO) ?: return emptyList()
        return List(data.size / INTS_PER_MOVE) { i ->
            val base = i * INTS_PER_MOVE
            SudokuMove(index = data[base], value = data[base + 1], notes = data[base + 2].toNoteSet())
        }
    }

    fun clear(handle: SavedStateHandle) {
        listOf(KEY_SOLUTION, KEY_GIVEN, KEY_CURRENT, KEY_NOTES, KEY_UNDO).forEach { handle.remove<Any>(it) }
    }

    private fun Set<Int>.toBitmask(): Int = fold(0) { mask, n -> mask or (1 shl n) }

    private fun Int.toNoteSet(): Set<Int> = (1..9).filter { this and (1 shl it) != 0 }.toSet()
}
