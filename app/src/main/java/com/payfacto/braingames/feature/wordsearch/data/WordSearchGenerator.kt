package com.payfacto.braingames.feature.wordsearch.data

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.wordsearch.model.Direction
import com.payfacto.braingames.feature.wordsearch.model.Puzzle
import com.payfacto.braingames.feature.wordsearch.model.WordEntry
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard

object WordSearchGenerator {

    private val ALL_DIRS = Direction.entries.toList()
    private const val MAX_ATTEMPTS = 60
    private const val SEARCH_LIMIT = 900
    private const val POOL_MULTIPLIER = 3

    fun gridSizeFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.VERY_EASY -> 8
        Difficulty.EASY      -> 10
        Difficulty.MEDIUM    -> 12
        Difficulty.HARD      -> 14
        Difficulty.EXPERT    -> 16
    }

    /**
     * Generate a mot-caché board, or null if no valid arrangement was found within the
     * attempt budget. Every returned board satisfies [WordSearchBoard.verify]: word cells and
     * hidden-word cells are disjoint and together cover the entire grid — there is never any
     * random filler. Callers must fall back to a different puzzle or a hardcoded board on null.
     */
    fun generate(words: List<String>, puzzle: Puzzle, difficulty: Difficulty): WordSearchBoard? {
        val gridSize = gridSizeFor(difficulty)
        val hiddenLen = puzzle.hiddenWord.length
        if (hiddenLen > gridSize * gridSize) return null

        val usableWords = words.filter { it.length in 4..gridSize && it != puzzle.hiddenWord }
        if (usableWords.isEmpty()) return null

        repeat(MAX_ATTEMPTS) {
            val attempt = attemptBuild(usableWords, gridSize, hiddenLen) ?: return@repeat
            val board = buildBoard(puzzle, attempt.first, attempt.second, gridSize)
            if (board.verify()) return board
        }
        return null
    }

    // ── Core placement loop ─────────────────────────────────────────────────
    //
    // Places words one at a time, always picking the placement that consumes the fewest
    // new (previously empty) cells, and never allowing the remaining empty count to drop
    // below hiddenLen. Stops exactly when hiddenLen empty cells remain, so the hidden word
    // can be written into them with zero random filler. Returns null if this particular
    // random ordering gets stuck (e.g. every remaining word would overshoot the budget) —
    // the caller retries with a fresh shuffle.

    private fun attemptBuild(
        words: List<String>,
        gridSize: Int,
        hiddenLen: Int
    ): Pair<Array<CharArray>, List<WordEntry>>? {
        val grid = Array(gridSize) { CharArray(gridSize) { ' ' } }
        val placed = mutableListOf<WordEntry>()
        var pool = (0 until POOL_MULTIPLIER).flatMap { words }.shuffled()
        var idx = 0
        var reshuffles = 0

        while (true) {
            val empty = countEmpty(grid)
            if (empty == hiddenLen) return grid to placed
            if (empty < hiddenLen) return null

            val maxNew = empty - hiddenLen

            if (idx >= pool.size) {
                if (reshuffles >= 4) return null
                pool = pool.shuffled()
                idx = 0
                reshuffles++
            }

            val word = pool[idx++]
            val (row, col, dir) = findBestPlacement(grid, word, gridSize, maxNew) ?: continue
            placeWord(grid, word, row, col, dir)
            placed.add(WordEntry(word, row, col, dir))
        }
    }

    // Search every direction/position (shuffled) and return the placement using the fewest
    // new cells within [1, maxNewCells]. Bounded by SEARCH_LIMIT checks for performance.
    private fun findBestPlacement(
        grid: Array<CharArray>,
        word: String,
        size: Int,
        maxNewCells: Int
    ): Triple<Int, Int, Direction>? {
        if (maxNewCells <= 0) return null

        var best: Triple<Int, Int, Direction>? = null
        var bestNewCount = maxNewCells + 1
        var checked = 0

        for (dir in ALL_DIRS.shuffled()) {
            for (r in (0 until size).shuffled()) {
                for (c in (0 until size).shuffled()) {
                    if (++checked > SEARCH_LIMIT) return best

                    val newCount = countNewCellsIfValid(grid, word, r, c, dir, size) ?: continue
                    if (newCount in 1..maxNewCells && newCount < bestNewCount) {
                        best = Triple(r, c, dir)
                        bestNewCount = newCount
                        if (newCount == 1) return best
                    }
                }
            }
        }
        return best
    }

    // Returns the number of new (empty) cells this placement would use, or null if invalid.
    private fun countNewCellsIfValid(
        grid: Array<CharArray>, word: String,
        row: Int, col: Int, dir: Direction, size: Int
    ): Int? {
        var newCount = 0
        for (i in word.indices) {
            val r = row + dir.dRow * i
            val c = col + dir.dCol * i
            if (r !in 0 until size || c !in 0 until size) return null
            val existing = grid[r][c]
            if (existing != ' ' && existing != word[i]) return null
            if (existing == ' ') newCount++
        }
        return newCount
    }

    private fun placeWord(grid: Array<CharArray>, word: String, row: Int, col: Int, dir: Direction) {
        for (i in word.indices) grid[row + dir.dRow * i][col + dir.dCol * i] = word[i]
    }

    private fun countEmpty(grid: Array<CharArray>): Int =
        grid.sumOf { row -> row.count { it == ' ' } }

    private fun buildBoard(
        puzzle: Puzzle,
        grid: Array<CharArray>,
        placed: List<WordEntry>,
        size: Int
    ): WordSearchBoard {
        val work = grid.map { it.copyOf() }.toTypedArray()
        val hiddenCells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until size) for (c in 0 until size) {
            if (work[r][c] == ' ') hiddenCells.add(Pair(r, c))
        }

        // Write the hidden word into the leftover cells in reading order (top→bottom, left→right).
        for ((i, cell) in hiddenCells.withIndex()) {
            work[cell.first][cell.second] = puzzle.hiddenWord[i]
        }

        return WordSearchBoard(
            grid = work,
            words = placed,
            theme = puzzle.theme,
            hiddenWord = puzzle.hiddenWord,
            hint = puzzle.hint,
            hiddenWordCells = hiddenCells
        )
    }
}
