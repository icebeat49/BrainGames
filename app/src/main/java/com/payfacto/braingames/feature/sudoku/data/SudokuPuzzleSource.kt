package com.payfacto.braingames.feature.sudoku.data

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.sudoku.model.SudokuBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Supplies new Sudoku puzzles. An interface so ViewModel tests can use a fixed board. */
interface SudokuPuzzleSource {
    suspend fun newPuzzle(difficulty: Difficulty): SudokuBoard
}

class GeneratedSudokuPuzzleSource @Inject constructor() : SudokuPuzzleSource {
    override suspend fun newPuzzle(difficulty: Difficulty): SudokuBoard =
        withContext(Dispatchers.Default) { SudokuGenerator.generate(difficulty) }
}
