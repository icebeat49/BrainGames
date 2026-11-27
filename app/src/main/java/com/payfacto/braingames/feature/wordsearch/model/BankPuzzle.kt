package com.payfacto.braingames.feature.wordsearch.model

/**
 * A bundled Word Search puzzle. [letters] is the grid read row by row. Each of [words] appears
 * exactly once in it, in one of the eight directions; the letters no word covers, read left to
 * right and top to bottom, spell [secret] without its spaces.
 */
data class BankPuzzle(
    val size: Int,
    val letters: String,
    val words: List<String>,
    /** As shown to the player: a word, or a phrase with single spaces ("WELL DONE"). */
    val secret: String,
) {
    init {
        require(letters.length == size * size) { "a ${size}x$size grid needs ${size * size} letters, got ${letters.length}" }
    }

    /** The secret as written in the grid. */
    val secretLetters: String get() = secretLettersOf(secret)

    companion object {
        fun secretLettersOf(secret: String): String = secret.replace(" ", "")
    }

    fun letterAt(row: Int, col: Int): Char = letters[row * size + col]

    /** Every place [word] can be read in the grid. A valid puzzle has exactly one per word. */
    fun placementsOf(word: String): List<WordEntry> = LetterGrid.placements(letters, size, word)

    /** The playable board: each word at its single placement, the leftover cells holding [secret]. */
    fun toBoard(): WordSearchBoard {
        val entries = words.map { placementsOf(it).single() }
        val covered = entries.flatMap { it.cells() }.toSet()
        val secretCells = (0 until size).flatMap { row -> (0 until size).map { col -> row to col } }
            .filterNot { it in covered }
        return WordSearchBoard(
            grid = Array(size) { row -> CharArray(size) { col -> letterAt(row, col) } },
            words = entries,
            hiddenWord = secret,
            hiddenWordCells = secretCells,
        )
    }
}

/** Finds words in a square grid of letters stored row by row. */
object LetterGrid {
    fun placements(letters: CharSequence, size: Int, word: String): List<WordEntry> {
        val found = mutableListOf<WordEntry>()
        for (row in 0 until size) for (col in 0 until size) for (direction in Direction.entries) {
            val entry = WordEntry(word, row, col, direction)
            if (readsAt(letters, size, entry)) found += entry
        }
        return found
    }

    private fun readsAt(letters: CharSequence, size: Int, entry: WordEntry): Boolean {
        val last = entry.word.length - 1
        val endRow = entry.startRow + entry.direction.dRow * last
        val endCol = entry.startCol + entry.direction.dCol * last
        if (endRow !in 0 until size || endCol !in 0 until size) return false
        return entry.word.indices.all { i ->
            val row = entry.startRow + entry.direction.dRow * i
            val col = entry.startCol + entry.direction.dCol * i
            letters[row * size + col] == entry.word[i]
        }
    }
}
