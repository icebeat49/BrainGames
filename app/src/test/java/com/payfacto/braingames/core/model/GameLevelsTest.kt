package com.payfacto.braingames.core.model

import com.payfacto.braingames.feature.crowns.engine.CrownsDifficulty
import com.payfacto.braingames.feature.wordsearch.data.WordSearchDifficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class GameLevelsTest {

    @Test
    fun `Crowns adds a Master level, 12x12, above Expert`() {
        assertEquals(Difficulty.STANDARD + Difficulty.MASTER, GameType.CROWNS.levels)
        assertEquals(12, CrownsDifficulty.sizeFor(Difficulty.MASTER))
    }

    @Test
    fun `Word Search offers Medium 12x12 and Expert 18x18`() {
        assertEquals(listOf(Difficulty.MEDIUM, Difficulty.EXPERT), GameType.WORD_SEARCH.levels)
        assertEquals(listOf(12, 18), GameType.WORD_SEARCH.levels.map(WordSearchDifficulty::sizeFor))
    }

    @Test
    fun `Sudoku and Kakuro keep the five standard levels`() {
        listOf(GameType.SUDOKU, GameType.KAKURO).forEach { game ->
            assertEquals(game.name, Difficulty.STANDARD, game.levels)
        }
    }
}
