package com.payfacto.braingames.feature.wordsearch

import com.payfacto.braingames.feature.wordsearch.WordSearchBankFiles.LANGUAGES
import com.payfacto.braingames.feature.wordsearch.WordSearchBankFiles.SIZES
import com.payfacto.braingames.feature.wordsearch.data.WordSearchBankFormat
import com.payfacto.braingames.feature.wordsearch.data.WordSearchBankRules
import com.payfacto.braingames.feature.wordsearch.model.BankPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every puzzle shipped in res/raw is checked against the rules it was generated with. */
class WordSearchBankTest {

    @Test
    fun `each bank ships 100 distinct puzzles of its size`() {
        forEachBank { language, size, puzzles ->
            assertEquals("$language $size count", BANK_SIZE, puzzles.size)
            assertEquals("$language $size distinct", BANK_SIZE, puzzles.map { it.letters }.toSet().size)
            assertTrue("$language $size size", puzzles.all { it.size == size })
        }
    }

    @Test
    fun `every word appears exactly once`() {
        forEachPuzzle { label, puzzle ->
            puzzle.words.forEach { word ->
                assertEquals("$label: $word", 1, puzzle.placementsOf(word).size)
            }
        }
    }

    @Test
    fun `the letters no word covers spell the secret, and nothing else is left`() {
        forEachPuzzle { label, puzzle ->
            val board = puzzle.toBoard()
            assertTrue("$label: every cell is a word or the secret", board.verify())
            val spelled = board.hiddenWordCells.joinToString("") { (row, col) -> puzzle.letterAt(row, col).toString() }
            assertEquals(label, puzzle.secretLetters, spelled)
        }
    }

    @Test
    fun `no secret is used twice in a language`() {
        LANGUAGES.forEach { language ->
            val secrets = SIZES.flatMap { size -> WordSearchBankFiles.load(language, size).map { it.secretLetters } }
            val repeated = secrets.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

            assertTrue("$language secrets used more than once: $repeated", repeated.isEmpty())
        }
    }

    @Test
    fun `words and secrets come from the lists and follow the length rules`() {
        forEachBank { language, size, puzzles ->
            val words = WordSearchBankFiles.words(language).toSet()
            val secrets = WordSearchBankFiles.secrets(language).toSet()
            val secretLengths = WordSearchBankFiles.secretLengthsFor(size)
            puzzles.forEachIndexed { index, puzzle ->
                val label = "$language ${size}x$size #$index"
                assertTrue("$label words in list", words.containsAll(puzzle.words))
                assertEquals("$label words distinct", puzzle.words.size, puzzle.words.toSet().size)
                assertTrue("$label word lengths", puzzle.words.all { it.length in WordSearchBankRules.MIN_WORD_LENGTH..size })
                assertTrue("$label secret ${puzzle.secret}", puzzle.secret in secrets && puzzle.secretLetters.length in secretLengths)
            }
        }
    }

    @Test
    fun `word lists are clean`() {
        LANGUAGES.forEach { language ->
            val words = WordSearchBankFiles.words(language)
            val secrets = WordSearchBankFiles.secrets(language)
            words.forEach { assertTrue("$language: word '$it' is A-Z only", it.matches(Regex("[A-Z]+"))) }
            secrets.forEach { assertTrue("$language: secret '$it' is A-Z words, single spaces", it.matches(Regex("[A-Z]+( [A-Z]+)*"))) }
            val secretLetters = secrets.map { BankPuzzle.secretLettersOf(it) }
            assertEquals("$language words distinct", words.size, words.toSet().size)
            assertEquals("$language secrets distinct once spaces are dropped", secretLetters.size, secretLetters.toSet().size)
            assertTrue("$language: a secret is also a word to find", words.intersect(secretLetters.toSet()).isEmpty())
        }
    }

    @Test
    fun `a bank line reads back to the same puzzle, phrase secrets included`() {
        val puzzles = WordSearchBankFiles.load("en", 18)
        val withPhrase = puzzles.first { ' ' in it.secret }

        listOf(puzzles.first(), withPhrase).forEach { puzzle ->
            assertEquals(puzzle, WordSearchBankFormat.decode(WordSearchBankFormat.encode(puzzle)))
        }
    }

    private fun forEachBank(check: (String, Int, List<BankPuzzle>) -> Unit) {
        LANGUAGES.forEach { language -> SIZES.forEach { size -> check(language, size, WordSearchBankFiles.load(language, size)) } }
    }

    private fun forEachPuzzle(check: (String, BankPuzzle) -> Unit) {
        forEachBank { language, size, puzzles ->
            puzzles.forEachIndexed { index, puzzle -> check("$language ${size}x$size #$index", puzzle) }
        }
    }

    private companion object {
        const val BANK_SIZE = 100
    }
}
