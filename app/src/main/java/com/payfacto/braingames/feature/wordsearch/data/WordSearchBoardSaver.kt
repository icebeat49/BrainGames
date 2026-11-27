package com.payfacto.braingames.feature.wordsearch.data

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.feature.wordsearch.model.Direction
import com.payfacto.braingames.feature.wordsearch.model.WordEntry
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard
import com.payfacto.braingames.feature.wordsearch.model.WordSearchEdit

/**
 * Stores the in-progress board in a [SavedStateHandle] using Bundle-friendly types.
 * Each word is encoded as `WORD|row|col|DIRECTION|found`. Words are plain letters,
 * so the separator never collides.
 */
internal object WordSearchBoardSaver {
    private const val KEY_GRID = "ws_grid"
    private const val KEY_WORDS = "ws_words"
    private const val KEY_HIDDEN_WORD = "ws_hidden_word"
    private const val KEY_HIDDEN_CELLS = "ws_hidden_cells"
    private const val KEY_SECRET_FOUND = "ws_secret_found"
    private const val KEY_MARKS = "ws_marks"
    private const val KEY_UNDO = "ws_undo"
    private const val EDIT_SEPARATOR = "/"
    private const val SEPARATOR = "|"

    fun save(handle: SavedStateHandle, board: WordSearchBoard) {
        handle[KEY_GRID] = Array(board.rows) { r -> String(board.grid[r]) }
        handle[KEY_WORDS] = board.words.map { it.encode() }.toTypedArray()
        handle[KEY_HIDDEN_WORD] = board.hiddenWord
        handle[KEY_HIDDEN_CELLS] = board.hiddenWordCells
            .flatMap { (r, c) -> listOf(r, c) }
            .toIntArray()
        handle[KEY_SECRET_FOUND] = board.secretFound
        handle[KEY_MARKS] = board.marks.map(::encodeLine).toTypedArray()
    }

    /** One string per undo step, oldest first: its edits joined by `/`. */
    fun saveUndo(handle: SavedStateHandle, steps: List<List<WordSearchEdit>>) {
        handle[KEY_UNDO] = steps.map { step -> step.joinToString(EDIT_SEPARATOR, transform = WordSearchEdit::encode) }.toTypedArray()
    }

    fun restoreUndo(handle: SavedStateHandle): List<List<WordSearchEdit>> =
        handle.get<Array<String>>(KEY_UNDO).orEmpty().map { step -> step.split(EDIT_SEPARATOR).map(WordSearchEdit::decode) }

    fun restore(handle: SavedStateHandle): WordSearchBoard? {
        val grid = handle.get<Array<String>>(KEY_GRID) ?: return null
        val words = handle.get<Array<String>>(KEY_WORDS) ?: return null
        val hiddenCells = handle.get<IntArray>(KEY_HIDDEN_CELLS) ?: return null
        return WordSearchBoard(
            grid = Array(grid.size) { r -> grid[r].toCharArray() },
            words = words.map { decodeWord(it) },
            hiddenWord = handle[KEY_HIDDEN_WORD] ?: "",
            hiddenWordCells = hiddenCells.toList().chunked(2) { (r, c) -> Pair(r, c) },
            secretFound = handle[KEY_SECRET_FOUND] ?: false,
            marks = handle.get<Array<String>>(KEY_MARKS).orEmpty().map(::decodeLine),
        )
    }

    fun clear(handle: SavedStateHandle) {
        listOf(KEY_GRID, KEY_WORDS, KEY_HIDDEN_WORD, KEY_HIDDEN_CELLS, KEY_SECRET_FOUND, KEY_MARKS, KEY_UNDO)
            .forEach { handle.remove<Any>(it) }
    }

    /** `r,c;r,c;...` */
    private fun encodeLine(cells: List<Pair<Int, Int>>): String = cells.joinToString(";") { (r, c) -> "$r,$c" }

    private fun decodeLine(encoded: String): List<Pair<Int, Int>> =
        encoded.split(';').map { cell -> cell.split(',').let { (r, c) -> r.toInt() to c.toInt() } }

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
