package com.payfacto.braingames.feature.wordsearch.model

import java.text.Normalizer

data class WordSearchBoard(
    val grid: Array<CharArray>,
    val words: List<WordEntry>,
    /** As shown to the player: a word, or a phrase with single spaces ("WELL DONE"). */
    val hiddenWord: String = "",
    /** The cells no word covers, in reading order: they spell [hiddenWord] without its spaces. */
    val hiddenWordCells: List<Pair<Int, Int>> = emptyList(),
    /** True once the player typed the secret correctly: that is what wins the game. */
    val secretFound: Boolean = false,
) {
    val rows: Int get() = grid.size
    val cols: Int get() = if (grid.isEmpty()) 0 else grid[0].size

    fun allWordsFound(): Boolean = words.all { it.found }

    /** Letters in the secret, spaces not counted. */
    val secretLetterCount: Int get() = hiddenWordCells.size

    val secretWordCount: Int get() = hiddenWord.split(' ').count { it.isNotEmpty() }

    /** Whether [guess] is the secret, ignoring case, accents, spaces and punctuation. */
    fun isSecret(guess: String): Boolean = lettersOnly(guess) == lettersOnly(hiddenWord)

    /**
     * A puzzle is only valid if every cell belongs to exactly one of:
     * a listed word, or the hidden word. No cell may be both, none may be neither.
     */
    fun verify(): Boolean {
        val wordCells = words.flatMap { it.cells() }.toSet()
        val hiddenCells = hiddenWordCells.toSet()
        val totalCells = rows * cols
        return wordCells.intersect(hiddenCells).isEmpty() &&
            wordCells.size + hiddenCells.size == totalCells &&
            grid.all { row -> row.all { it != ' ' } }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WordSearchBoard) return false
        return grid.contentDeepEquals(other.grid) &&
                words == other.words &&
                hiddenWord == other.hiddenWord &&
                secretFound == other.secretFound
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + words.hashCode()
        result = 31 * result + hiddenWord.hashCode()
        result = 31 * result + secretFound.hashCode()
        return result
    }

    private companion object {
        val ACCENTS = Regex("\\p{Mn}+")
        val NOT_A_LETTER = Regex("[^A-Z]")

        fun lettersOnly(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(ACCENTS, "")
                .uppercase()
                .replace(NOT_A_LETTER, "")
    }
}
