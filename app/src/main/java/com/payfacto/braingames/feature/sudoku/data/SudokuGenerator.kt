package com.payfacto.braingames.feature.sudoku.data

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.sudoku.model.SudokuBoard

object SudokuGenerator {

    fun generate(difficulty: Difficulty): SudokuBoard {
        if (difficulty >= Difficulty.HARD) {
            repeat(50) {
                val candidate = generateRaw(difficulty)
                if (matchesTechniqueLevel(candidate.current, difficulty)) return candidate
            }
        }
        return generateRaw(difficulty)
    }

    private fun matchesTechniqueLevel(puzzle: IntArray, difficulty: Difficulty): Boolean {
        val level = SudokuAnalyzer.analyzeRequiredLevel(puzzle)
        return when (difficulty) {
            Difficulty.HARD ->
                level.ordinal in TechniqueLevel.NAKED_PAIR.ordinal..TechniqueLevel.HIDDEN_TRIPLE.ordinal
            Difficulty.EXPERT, Difficulty.MASTER ->
                level.ordinal >= TechniqueLevel.X_WING.ordinal
            else -> true
        }
    }

    private fun generateRaw(difficulty: Difficulty): SudokuBoard {
        val solution = IntArray(81)
        fillGrid(solution)

        val current = solution.copyOf()
        val given = BooleanArray(81) { true }

        val targetGivens = when (difficulty) {
            Difficulty.VERY_EASY -> (50..55).random()
            Difficulty.EASY      -> (40..49).random()
            Difficulty.MEDIUM    -> (32..39).random()
            Difficulty.HARD      -> (26..31).random()
            Difficulty.EXPERT, Difficulty.MASTER -> (20..25).random()
        }

        val indices = (0 until 81).toMutableList().also { it.shuffle() }
        var givensLeft = 81

        for (idx in indices) {
            if (givensLeft <= targetGivens) break
            val backup = current[idx]
            current[idx] = 0
            given[idx] = false
            if (!hasUniqueSolution(current.copyOf())) {
                current[idx] = backup
                given[idx] = true
            } else {
                givensLeft--
            }
        }

        return SudokuBoard(
            solution = solution,
            given = given,
            current = current
        )
    }

    private fun fillGrid(grid: IntArray): Boolean {
        val empty = grid.indexOfFirst { it == 0 }
        if (empty == -1) return true
        val digits = (1..9).shuffled()
        for (d in digits) {
            if (isValid(grid, empty, d)) {
                grid[empty] = d
                if (fillGrid(grid)) return true
                grid[empty] = 0
            }
        }
        return false
    }

    private fun isValid(grid: IntArray, index: Int, value: Int): Boolean {
        val row = index / 9
        val col = index % 9
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (i in 0 until 9) {
            if (grid[row * 9 + i] == value) return false
            if (grid[i * 9 + col] == value) return false
            if (grid[(boxRow + i / 3) * 9 + (boxCol + i % 3)] == value) return false
        }
        return true
    }

    private fun hasUniqueSolution(grid: IntArray): Boolean {
        val count = intArrayOf(0)
        countSolutions(grid.copyOf(), count)
        return count[0] == 1
    }

    private fun countSolutions(grid: IntArray, count: IntArray) {
        if (count[0] > 1) return
        val empty = grid.indexOfFirst { it == 0 }
        if (empty == -1) {
            count[0]++
            return
        }
        for (d in 1..9) {
            if (isValid(grid, empty, d)) {
                grid[empty] = d
                countSolutions(grid, count)
                grid[empty] = 0
            }
        }
    }
}
