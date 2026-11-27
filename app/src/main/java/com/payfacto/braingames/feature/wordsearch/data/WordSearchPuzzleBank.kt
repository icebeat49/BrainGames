package com.payfacto.braingames.feature.wordsearch.data

import android.content.Context
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.feature.wordsearch.model.BankPuzzle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Grid size per level: Word Search offers Medium 12×12 and Expert 18×18 ([GameType.WORD_SEARCH]). */
object WordSearchDifficulty {
    /** Levels at or below Medium (an older saved choice included) play 12×12, the others 18×18. */
    fun sizeFor(difficulty: Difficulty): Int = if (difficulty <= Difficulty.MEDIUM) 12 else 18
}

/**
 * The bundled, pre-verified puzzles per language and grid size, read once from res/raw.
 * Generated offline by WordSearchBankWriter (unit tests); WordSearchBankTest re-verifies them.
 */
@Singleton
class WordSearchPuzzleBank @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = ConcurrentHashMap<Pair<String, Int>, List<BankPuzzle>>()

    /** Blocking file read on first use per bank: call off the main thread. [language] is "en" or "fr". */
    fun puzzles(language: String, size: Int): List<BankPuzzle> = cache.getOrPut(language to size) {
        val text = context.resources.openRawResource(rawIdFor(language, size)).bufferedReader().use { it.readText() }
        WordSearchBankFormat.parse(text)
    }

    private fun rawIdFor(language: String, size: Int): Int = when (language to size) {
        "en" to 12 -> R.raw.wordsearch_bank_en_12
        "en" to 18 -> R.raw.wordsearch_bank_en_18
        "fr" to 12 -> R.raw.wordsearch_bank_fr_12
        "fr" to 18 -> R.raw.wordsearch_bank_fr_18
        else -> error("No Word Search bank for $language ${size}x$size")
    }
}
