package com.payfacto.braingames.feature.stats

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.stats.GameStats
import com.payfacto.braingames.core.stats.StatsKey
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsSectionsTest {

    private val recorded = mapOf(
        StatsKey(GameType.SUDOKU, Difficulty.EASY, GameMode.CLASSIC) to
            GameStats(played = 3, won = 2, bestMs = 60_000, totalWonMs = 150_000),
        StatsKey(GameType.SUDOKU, Difficulty.HARD, GameMode.CLASSIC) to GameStats(played = 1),
        StatsKey(GameType.SUDOKU, Difficulty.EASY, GameMode.HARDCORE) to
            GameStats(played = 2, won = 1, bestMs = 45_000, totalWonMs = 45_000)
    )

    private val sections = StatsViewModel.sectionsFor(recorded, listOf(GameType.SUDOKU, GameType.KAKURO))
    private val sudoku = sections.first { it.game == GameType.SUDOKU }
    private val kakuro = sections.first { it.game == GameType.KAKURO }

    @Test
    fun `a game with modes shows both, a game without shows Classic only`() {
        assertEquals(GameMode.entries.toSet(), sudoku.modes.keys)
        assertEquals(setOf(GameMode.CLASSIC), kakuro.modes.keys)
    }

    @Test
    fun `every level has a row, zero when never played`() {
        val classic = sudoku.modes.getValue(GameMode.CLASSIC).byDifficulty
        assertEquals(Difficulty.entries, classic.keys.toList())
        assertEquals(GameStats(), classic.getValue(Difficulty.MEDIUM))
    }

    @Test
    fun `a mode's total adds up its levels`() {
        assertEquals(
            GameStats(played = 4, won = 2, bestMs = 60_000, totalWonMs = 150_000),
            sudoku.modes.getValue(GameMode.CLASSIC).total
        )
    }

    @Test
    fun `a game's total adds up every level of every mode`() {
        assertEquals(GameStats(played = 6, won = 3, bestMs = 45_000, totalWonMs = 195_000), sudoku.total)
        assertEquals(GameStats(), kakuro.total)
    }
}
