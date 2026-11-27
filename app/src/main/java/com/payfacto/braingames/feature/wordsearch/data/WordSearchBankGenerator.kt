package com.payfacto.braingames.feature.wordsearch.data

import com.payfacto.braingames.feature.wordsearch.model.BankPuzzle
import com.payfacto.braingames.feature.wordsearch.model.Direction
import com.payfacto.braingames.feature.wordsearch.model.LetterGrid
import kotlin.random.Random

/** The grid size of one bank and the words its grids are built from. */
class WordSearchBankRules(val size: Int, words: List<String>) {
    /** Words that fit the grid. Palindromes are left out: they read the same both ways, so twice. */
    val words: List<String> = words.distinct().filter { it.length in MIN_WORD_LENGTH..size && it != it.reversed() }

    companion object {
        /** Shorter words turn up again by chance in a big grid, breaking "appears exactly once". */
        const val MIN_WORD_LENGTH = 4
    }
}

/** Where a generated grid takes its secret from. Lengths count letters only, not spaces. */
interface SecretPicker {
    /** The secret lengths still on offer. */
    fun lengths(): List<Int>

    /** A secret with [length] letters, or null when none is left. */
    fun pick(length: Int): String?
}

/**
 * Builds puzzles where every cell belongs to a word or to the secret, with no random filler:
 * 1. pick a target secret length;
 * 2. place words one at a time, each where it adds the fewest new letters, never leaving fewer
 *    empty cells than the target, until no word fits;
 * 3. if a secret has as many letters as there are empty cells, write it into them in reading order;
 * 4. keep the grid only if every word appears exactly once.
 */
class WordSearchBankGenerator(private val random: Random = Random.Default) {

    /** One puzzle, or null when this attempt failed (call again). */
    fun generate(rules: WordSearchBankRules, secrets: SecretPicker): BankPuzzle? {
        val size = rules.size
        val target = secrets.lengths().randomOrNull(random) ?: return null
        val grid = CharArray(size * size) { EMPTY }
        val placed = fillWithWords(rules, grid, target)
        val secret = secrets.pick(grid.count { it == EMPTY }) ?: return null
        writeSecret(grid, BankPuzzle.secretLettersOf(secret))
        val letters = String(grid)
        if (placed.any { LetterGrid.placements(letters, size, it).size != 1 }) return null
        return BankPuzzle(size, letters, placed, secret)
    }

    private fun fillWithWords(rules: WordSearchBankRules, grid: CharArray, target: Int): List<String> {
        val placed = LinkedHashSet<String>()
        var empty = grid.size
        var progress = true
        while (progress && empty > target) {
            progress = false
            for (word in rules.words.filterNot { it in placed }.shuffled(random)) {
                val best = bestPlacement(grid, rules.size, word, maxNewCells = empty - target) ?: continue
                best.writeInto(grid, rules.size, word)
                empty -= best.newCells
                placed += word
                progress = true
                if (empty == target) break
            }
        }
        return placed.toList()
    }

    /** Where [word] adds the fewest new letters (1 to [maxNewCells]), ties broken at random. */
    private fun bestPlacement(grid: CharArray, size: Int, word: String, maxNewCells: Int): Placement? {
        var best: Placement? = null
        var ties = 0
        for (row in 0 until size) for (col in 0 until size) for (direction in Direction.entries) {
            val newCells = newCellsAt(grid, size, word, Placement(row, col, direction, 0)) ?: continue
            if (newCells !in 1..maxNewCells) continue
            val bestNew = best?.newCells ?: Int.MAX_VALUE
            if (newCells < bestNew) {
                best = Placement(row, col, direction, newCells)
                ties = 1
            } else if (newCells == bestNew && random.nextInt(++ties) == 0) {
                best = Placement(row, col, direction, newCells)
            }
        }
        return best
    }

    /** Empty cells [word] would fill at [at], or null if it leaves the grid or clashes with a letter. */
    private fun newCellsAt(grid: CharArray, size: Int, word: String, at: Placement): Int? {
        val last = word.length - 1
        if (at.row + at.direction.dRow * last !in 0 until size || at.col + at.direction.dCol * last !in 0 until size) return null
        var newCells = 0
        for (i in word.indices) {
            val existing = grid[at.cellIndex(size, i)]
            if (existing == EMPTY) newCells++ else if (existing != word[i]) return null
        }
        return newCells
    }

    private fun writeSecret(grid: CharArray, secretLetters: String) {
        var next = 0
        for (cell in grid.indices) if (grid[cell] == EMPTY) grid[cell] = secretLetters[next++]
    }

    private data class Placement(val row: Int, val col: Int, val direction: Direction, val newCells: Int) {
        fun cellIndex(size: Int, i: Int): Int = (row + direction.dRow * i) * size + col + direction.dCol * i

        fun writeInto(grid: CharArray, size: Int, word: String) {
            for (i in word.indices) grid[cellIndex(size, i)] = word[i]
        }
    }

    private companion object {
        const val EMPTY = '\u0000'
    }
}
