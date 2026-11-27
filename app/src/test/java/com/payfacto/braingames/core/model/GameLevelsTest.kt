package com.payfacto.braingames.core.model

import com.payfacto.braingames.feature.crowns.engine.CrownsDifficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class GameLevelsTest {

    @Test
    fun `Crowns adds a Master level, 12x12, above Expert`() {
        assertEquals(Difficulty.STANDARD + Difficulty.MASTER, GameType.CROWNS.levels)
        assertEquals(12, CrownsDifficulty.sizeFor(Difficulty.MASTER))
    }

    @Test
    fun `the other games keep the five standard levels`() {
        listOf(GameType.SUDOKU, GameType.KAKURO, GameType.WORD_SEARCH).forEach { game ->
            assertEquals(game.name, Difficulty.STANDARD, game.levels)
        }
    }
}
