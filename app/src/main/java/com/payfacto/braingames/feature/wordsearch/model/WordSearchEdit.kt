package com.payfacto.braingames.feature.wordsearch.model

/**
 * One change to a Word Search board. Each one is its own inverse (applying it twice changes
 * nothing), so undoing a step is applying its edits again, last first.
 */
sealed interface WordSearchEdit {
    fun applyTo(board: WordSearchBoard): WordSearchBoard

    /** Ticks [word] off the list, or back on. */
    data class TickWord(val word: String) : WordSearchEdit {
        override fun applyTo(board: WordSearchBoard): WordSearchBoard {
            val crossedOff = board.words.firstOrNull { it.word == word }?.found ?: return board
            return board.withWordCrossedOff(word, !crossedOff)
        }
    }

    /** Draws a line over [cells], or erases it when the same line is already drawn. */
    data class ToggleLine(val cells: List<Pair<Int, Int>>) : WordSearchEdit {
        override fun applyTo(board: WordSearchBoard): WordSearchBoard = board.withLineToggled(cells)
    }

    companion object {
        private const val WORD_PREFIX = "W:"
        private const val LINE_PREFIX = "L:"

        /** Text form for saved state: `W:WORD`, or `L:r,c;r,c;...`. */
        fun encode(edit: WordSearchEdit): String = when (edit) {
            is TickWord -> WORD_PREFIX + edit.word
            is ToggleLine -> LINE_PREFIX + edit.cells.joinToString(";") { (r, c) -> "$r,$c" }
        }

        fun decode(text: String): WordSearchEdit = when {
            text.startsWith(WORD_PREFIX) -> TickWord(text.removePrefix(WORD_PREFIX))
            text.startsWith(LINE_PREFIX) -> ToggleLine(
                text.removePrefix(LINE_PREFIX).split(';').map { cell -> cell.split(',').let { (r, c) -> r.toInt() to c.toInt() } }
            )
            else -> error("Unknown Word Search edit: $text")
        }
    }
}

/** Applies [edits] in order. */
fun WordSearchBoard.applying(edits: List<WordSearchEdit>): WordSearchBoard = edits.fold(this) { board, edit -> edit.applyTo(board) }
