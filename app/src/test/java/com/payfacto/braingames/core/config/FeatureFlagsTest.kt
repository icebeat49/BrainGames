package com.payfacto.braingames.core.config

import com.payfacto.braingames.core.model.GameType
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureFlagsTest {

    @Test
    fun `word search is hidden when its flag is off`() {
        val enabled = FeatureFlags.enabledGamesFor(wordSearchEnabled = false, animalsEnabled = true)

        assertEquals(setOf(GameType.SUDOKU, GameType.KAKURO, GameType.ANIMALS), enabled)
    }

    @Test
    fun `animals is hidden when its flag is off`() {
        val enabled = FeatureFlags.enabledGamesFor(wordSearchEnabled = true, animalsEnabled = false)

        assertEquals(setOf(GameType.SUDOKU, GameType.KAKURO, GameType.WORD_SEARCH), enabled)
    }

    @Test
    fun `a release build shows only Sudoku and Kakuro`() {
        val enabled = FeatureFlags.enabledGamesFor(wordSearchEnabled = false, animalsEnabled = false)

        assertEquals(setOf(GameType.SUDOKU, GameType.KAKURO), enabled)
    }

    @Test
    fun `every game is shown when every flag is on`() {
        val enabled = FeatureFlags.enabledGamesFor(wordSearchEnabled = true, animalsEnabled = true)

        assertEquals(GameType.entries.toSet(), enabled)
    }

    @Test
    fun `enabled games keep the GameType order used by the Home screen`() {
        val enabled = FeatureFlags.enabledGamesFor(wordSearchEnabled = true, animalsEnabled = true)

        assertEquals(GameType.entries, enabled.toList())
    }
}
