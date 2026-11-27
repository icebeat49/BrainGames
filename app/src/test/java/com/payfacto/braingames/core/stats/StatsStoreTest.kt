package com.payfacto.braingames.core.stats

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.testutil.fakeStatsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsStoreTest {

    private val store = fakeStatsStore()
    private val sudokuEasy = StatsKey(GameType.SUDOKU, Difficulty.EASY, GameMode.CLASSIC)
    private val sudokuEasyHardcore = StatsKey(GameType.SUDOKU, Difficulty.EASY, GameMode.HARDCORE)
    private val kakuroHard = StatsKey(GameType.KAKURO, Difficulty.HARD, GameMode.CLASSIC)

    private fun recorded(): Map<StatsKey, GameStats> = runBlocking { store.all.first() }

    @Test
    fun `nothing is recorded at first`() {
        assertEquals(emptyMap<StatsKey, GameStats>(), recorded())
    }

    @Test
    fun `played and won are counted, with best and total time`() {
        repeat(3) { store.recordPlayed(sudokuEasy) }
        store.recordWin(sudokuEasy, 90_000)
        store.recordWin(sudokuEasy, 60_000)

        assertEquals(GameStats(played = 3, won = 2, bestMs = 60_000, totalWonMs = 150_000), recorded()[sudokuEasy])
    }

    @Test
    fun `the average is the total time over the wins`() {
        assertEquals(75_000L, GameStats(won = 2, totalWonMs = 150_000).averageMs)
        assertNull(GameStats(played = 4).averageMs)
    }

    @Test
    fun `each level and mode is counted apart`() {
        store.recordPlayed(sudokuEasy)
        store.recordPlayed(sudokuEasyHardcore)
        store.recordPlayed(sudokuEasyHardcore)

        assertEquals(1, recorded()[sudokuEasy]?.played)
        assertEquals(2, recorded()[sudokuEasyHardcore]?.played)
    }

    @Test
    fun `resetting a game leaves the others`() {
        store.recordPlayed(sudokuEasy)
        store.recordPlayed(sudokuEasyHardcore)
        store.recordPlayed(kakuroHard)

        store.reset(GameType.SUDOKU)

        assertEquals(setOf(kakuroHard), recorded().keys)
    }

    @Test
    fun `reset all clears everything`() {
        store.recordPlayed(sudokuEasy)
        store.recordWin(kakuroHard, 1_000)

        store.resetAll()

        assertEquals(emptyMap<StatsKey, GameStats>(), recorded())
    }

    @Test
    fun `stats add up, keeping the best time`() {
        val sum = GameStats(played = 2, won = 1, bestMs = 50_000, totalWonMs = 50_000) +
            GameStats(played = 3, won = 2, bestMs = 40_000, totalWonMs = 100_000) +
            GameStats(played = 1)

        assertEquals(GameStats(played = 6, won = 3, bestMs = 40_000, totalWonMs = 150_000), sum)
    }
}
