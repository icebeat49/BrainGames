package com.payfacto.braingames.core.data

import androidx.datastore.preferences.core.stringPreferencesKey
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.testutil.FakePreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RecentPuzzlesTest {

    private val dataStore = FakePreferencesDataStore()

    @Test
    fun `no puzzle repeats within the last 50 picks of a 250 bank`() = runTest {
        val recent = RecentPuzzles(dataStore)
        val picks = List(500) { recent.pick(GameType.CROWNS, size = 11, count = 250, random = Random(it)) }

        picks.windowed(RecentPuzzles.MAX_RECENT + 1).forEach { window ->
            assertEquals("repeat in $window", window.size, window.toSet().size)
        }
        assertTrue(picks.all { it in 0 until 250 })
    }

    @Test
    fun `the window never exceeds half the bank`() {
        assertEquals(50, RecentPuzzles.windowFor(250))
        assertEquals(2, RecentPuzzles.windowFor(5))
        assertEquals(0, RecentPuzzles.windowFor(1))
    }

    @Test
    fun `a small bank still never repeats inside its window`() = runTest {
        val recent = RecentPuzzles(dataStore)
        val picks = List(60) { recent.pick(GameType.CROWNS, size = 7, count = 5, random = Random(it)) }

        picks.windowed(3).forEach { window -> assertEquals(3, window.toSet().size) }
    }

    @Test
    fun `recent picks are remembered across instances, like after a restart`() = runTest {
        val first = RecentPuzzles(dataStore).pick(GameType.CROWNS, size = 9, count = 2, random = Random(1))

        val second = RecentPuzzles(dataStore).pick(GameType.CROWNS, size = 9, count = 2, random = Random(2))

        assertTrue(first != second)
    }

    @Test
    fun `games are tracked separately`() = runTest {
        val recent = RecentPuzzles(dataStore)
        val crowns = recent.pick(GameType.CROWNS, size = 7, count = 250, random = Random(1))
        val kakuro = recent.pick(GameType.KAKURO, size = 7, count = 250, random = Random(2))

        val stored = dataStore.data.first()

        assertEquals(crowns.toString(), stored[stringPreferencesKey("crowns_recent_7")])
        assertEquals(kakuro.toString(), stored[stringPreferencesKey("kakuro_recent_7")])
    }

    @Test
    fun `sizes are tracked separately`() = runTest {
        val recent = RecentPuzzles(dataStore)
        val picked7 = recent.pick(GameType.CROWNS, size = 7, count = 250, random = Random(1))
        val picked8 = recent.pick(GameType.CROWNS, size = 8, count = 250, random = Random(2))

        val stored = dataStore.data.first()

        assertEquals(picked7.toString(), stored[stringPreferencesKey("crowns_recent_7")])
        assertEquals(picked8.toString(), stored[stringPreferencesKey("crowns_recent_8")])
    }
}
