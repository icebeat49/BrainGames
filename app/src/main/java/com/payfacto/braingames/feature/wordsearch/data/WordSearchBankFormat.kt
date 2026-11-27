package com.payfacto.braingames.feature.wordsearch.data

import com.payfacto.braingames.feature.wordsearch.model.BankPuzzle
import kotlin.math.sqrt

/**
 * Text format of the bundled puzzle banks (res/raw/wordsearch_bank_<lang>_<size>.txt): one
 * puzzle per line, space separated: the grid letters row by row, the secret (a phrase has '_'
 * between its words), then the words to find. Word positions are not stored: each word appears
 * exactly once in the grid, so it is found by searching. Lines starting with '#' and blank
 * lines are ignored.
 */
object WordSearchBankFormat {
    private const val COMMENT = '#'
    private const val SEPARATOR = ' '
    private const val SECRET_SPACE = '_'

    fun encode(puzzle: BankPuzzle): String {
        val secret = puzzle.secret.replace(SEPARATOR, SECRET_SPACE)
        return (listOf(puzzle.letters, secret) + puzzle.words).joinToString(SEPARATOR.toString())
    }

    fun decode(line: String): BankPuzzle {
        val fields = line.trim().split(SEPARATOR)
        require(fields.size >= 3) { "a puzzle line needs the grid, the secret and at least one word" }
        val letters = fields[0]
        val size = sqrt(letters.length.toDouble()).toInt()
        require(size * size == letters.length) { "the grid must be square, got ${letters.length} letters" }
        return BankPuzzle(size, letters, words = fields.drop(2), secret = fields[1].replace(SECRET_SPACE, SEPARATOR))
    }

    fun parse(text: String): List<BankPuzzle> =
        text.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith(COMMENT) }
            .map(::decode)
            .toList()

    fun write(puzzles: Collection<BankPuzzle>, header: String): String = buildString {
        header.lines().forEach { append(COMMENT).append(' ').append(it).append('\n') }
        puzzles.forEach { append(encode(it)).append('\n') }
    }
}
