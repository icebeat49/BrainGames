package com.payfacto.braingames.feature.wordsearch.data

import com.payfacto.braingames.core.data.RecentPuzzles
import com.payfacto.braingames.core.locale.AppLanguage
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Supplies new Word Search puzzles. An interface so ViewModel tests can use a fixed board. */
interface WordSearchPuzzleSource {
    suspend fun newPuzzle(difficulty: Difficulty): WordSearchBoard
}

/**
 * Serves puzzles from the bundled bank of the app language (French, or English for any other),
 * at random but never one of the recent ones for that size.
 */
class BundledWordSearchPuzzleSource @Inject constructor(
    private val bank: WordSearchPuzzleBank,
    private val recent: RecentPuzzles
) : WordSearchPuzzleSource {

    override suspend fun newPuzzle(difficulty: Difficulty): WordSearchBoard {
        val language = if (AppLanguage.displayLocale().language == FRENCH) FRENCH else ENGLISH
        return withContext(Dispatchers.IO) {
            val size = WordSearchDifficulty.sizeFor(difficulty)
            val puzzles = bank.puzzles(language, size)
            puzzles[recent.pick(GameType.WORD_SEARCH, size, puzzles.size)].toBoard()
        }
    }

    private companion object {
        const val FRENCH = "fr"
        const val ENGLISH = "en"
    }
}
