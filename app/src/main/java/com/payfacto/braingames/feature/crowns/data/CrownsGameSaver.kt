package com.payfacto.braingames.feature.crowns.data

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import com.payfacto.braingames.feature.crowns.model.CellMark

/**
 * Stores the game in a [SavedStateHandle] with Bundle-friendly types, so a ViewModel
 * recreated after process death resumes the same puzzle, marks and undo history.
 * Boards are stored as mark ordinals; the undo history is its boards concatenated.
 */
internal object CrownsGameSaver {
    private const val KEY_SIZE = "crowns_size"
    private const val KEY_REGIONS = "crowns_regions"
    private const val KEY_SOLUTION = "crowns_solution"
    private const val KEY_MARKS = "crowns_marks"
    private const val KEY_UNDO = "crowns_undo"
    private const val KEY_QUESTION_MODE = "crowns_question_mode"

    private val marks = CellMark.entries

    fun savePuzzle(handle: SavedStateHandle, puzzle: CrownsPuzzle) {
        handle[KEY_SIZE] = puzzle.size
        handle[KEY_REGIONS] = puzzle.regions
        handle[KEY_SOLUTION] = puzzle.solution
    }

    fun saveBoard(handle: SavedStateHandle, board: CrownsBoard) {
        handle[KEY_MARKS] = board.encode()
    }

    fun saveUndo(handle: SavedStateHandle, boards: List<CrownsBoard>) {
        handle[KEY_UNDO] = boards.flatMap { it.marks.map(CellMark::ordinal) }.toIntArray()
    }

    fun saveQuestionMode(handle: SavedStateHandle, enabled: Boolean) {
        handle[KEY_QUESTION_MODE] = enabled
    }

    fun restorePuzzle(handle: SavedStateHandle): CrownsPuzzle? {
        val size = handle.get<Int>(KEY_SIZE) ?: return null
        val regions = handle.get<IntArray>(KEY_REGIONS) ?: return null
        val solution = handle.get<IntArray>(KEY_SOLUTION) ?: return null
        return CrownsPuzzle(size, regions, solution)
    }

    fun restoreBoard(handle: SavedStateHandle, size: Int): CrownsBoard =
        handle.get<IntArray>(KEY_MARKS)?.let { decode(size, it.toList()) } ?: CrownsBoard.empty(size)

    fun restoreUndo(handle: SavedStateHandle, size: Int): List<CrownsBoard> {
        val data = handle.get<IntArray>(KEY_UNDO) ?: return emptyList()
        val cells = size * size
        if (cells == 0) return emptyList()
        return data.toList().chunked(cells).map { decode(size, it) }
    }

    fun restoreQuestionMode(handle: SavedStateHandle): Boolean = handle[KEY_QUESTION_MODE] ?: false

    fun clear(handle: SavedStateHandle) {
        listOf(KEY_SIZE, KEY_REGIONS, KEY_SOLUTION, KEY_MARKS, KEY_UNDO).forEach { handle.remove<Any>(it) }
    }

    private fun CrownsBoard.encode(): IntArray = marks.map(CellMark::ordinal).toIntArray()

    private fun decode(size: Int, ordinals: List<Int>) = CrownsBoard(size, ordinals.map { marks[it] })
}
