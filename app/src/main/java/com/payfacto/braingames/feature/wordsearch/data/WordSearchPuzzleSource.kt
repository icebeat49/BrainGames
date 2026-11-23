package com.payfacto.braingames.feature.wordsearch.data

import android.content.Context
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Supplies new Word Search puzzles. An interface so ViewModel tests can use a fixed board. */
interface WordSearchPuzzleSource {
    suspend fun newPuzzle(difficulty: Difficulty): WordSearchBoard
}

class GeneratedWordSearchPuzzleSource @Inject constructor(
    @ApplicationContext private val context: Context
) : WordSearchPuzzleSource {

    override suspend fun newPuzzle(difficulty: Difficulty): WordSearchBoard =
        withContext(Dispatchers.Default) {
            val gridSize = WordSearchGenerator.gridSizeFor(difficulty)
            val maxWordLen = gridSize.coerceAtMost(12)
            val words = WordBank.getWords(context, maxWordLen)
            val candidates = PuzzleBank.getPuzzles(context).shuffled()

            // Try each candidate puzzle until one yields a verified board (no random
            // filler is ever produced — a board either satisfies the rules or is null).
            candidates.firstNotNullOfOrNull { puzzle ->
                WordSearchGenerator.generate(words, puzzle, difficulty)
            } ?: FallbackPuzzles.get(context, difficulty)
        }
}
