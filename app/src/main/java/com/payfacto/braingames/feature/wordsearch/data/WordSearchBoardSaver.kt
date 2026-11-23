package com.payfacto.braingames.feature.wordsearch.data

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.feature.wordsearch.model.Direction
import com.payfacto.braingames.feature.wordsearch.model.WordEntry
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard

/**
 * Stores the in-progress board in a [SavedStateHandle] using Bundle-friendly types.
 * Each word is encoded as `WORD|row|col|DIRECTION|found`. Words are plain letters,
 * so the separator never collides.
 */
internal object WordSearchBoardSaver {
    private const val KEY_GRID = "ws_grid"
    private const val KEY_WORDS = "ws_words"
    private const val KEY_THEME = "ws_theme"
    private const val KEY_HIDDEN_WORD = "ws_hidden_word"
    private const val KEY_HINT = "ws_hint"
    private const val KEY_HIDDEN_CELLS = "ws_hidden_cells"
    private const val SEPARATOR = "|"

    fun save(handle: SavedStateHandle, board: WordSearchBoard) {
        handle[KEY_GRID] = Array(board.rows) { r -> String(board.grid[r]) }
        handle[KEY_WORDS] = board.words.map { it.encode() }.toTypedArray()
        handle[KEY_THEME] = board.theme
        handle[KEY_HIDDEN_WORD] = board.hiddenWord
        handle[KEY_HINT] = board.hint
        handle[KEY_HIDDEN_CELLS] = board.hiddenWordCells
            .flatMap { (r, c) -> listOf(r, c) }
            .toIntArray()
    }

    fun restore(handle: SavedStateHandle): WordSearchBoard? {
        val grid = handle.get<Array<String>>(KEY_GRID) ?: return null
        val words = handle.get<Array<String>>(KEY_WORDS) ?: return null
        val hiddenCells = handle.get<IntArray>(KEY_HIDDEN_CELLS) ?: return null
        return WordSearchBoard(
            grid = Array(grid.size) { r -> grid[r].toCharArray() },
            words = words.map { decodeWord(it) },
            theme = handle[KEY_THEME] ?: "",
            hiddenWord = handle[KEY_HIDDEN_WORD] ?: "",
            hint = handle[KEY_HINT] ?: "",
            hiddenWordCells = hiddenCells.toList().chunked(2) { (r, c) -> Pair(r, c) }
        )
    }

    fun clear(handle: SavedStateHandle) {
        listOf(KEY_GRID, KEY_WORDS, KEY_THEME, KEY_HIDDEN_WORD, KEY_HINT, KEY_HIDDEN_CELLS)
            .forEach { handle.remove<Any>(it) }
    }

    private fun WordEntry.encode(): String =
        listOf(word, startRow, startCol, direction.name, found).joinToString(SEPARATOR)

    private fun decodeWord(encoded: String): WordEntry {
        val (word, row, col, direction, found) = encoded.split(SEPARATOR)
        return WordEntry(
            word = word,
            startRow = row.toInt(),
            startCol = col.toInt(),
            direction = Direction.valueOf(direction),
            found = found.toBoolean()
        )
    }
}
