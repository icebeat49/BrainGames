package com.payfacto.braingames.feature.crowns

import androidx.datastore.preferences.core.stringPreferencesKey
import com.payfacto.braingames.feature.crowns.data.RecentCrownsPuzzles
import com.payfacto.braingames.testutil.FakePreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RecentCrownsPuzzlesTest {

    private val dataStore = FakePreferencesDataStore()

    @Test
    fun `no puzzle repeats within the last 50 picks of a 250 bank`() = runTest {
        val recent = RecentCrownsPuzzles(dataStore)
        val picks = List(500) { recent.pick(size = 11, count = 250, random = Random(it)) }

        picks.windowed(RecentCrownsPuzzles.MAX_RECENT + 1).forEach { window ->
            assertEquals("repeat in $window", window.size, window.toSet().size)
        }
        assertTrue(picks.all { it in 0 until 250 })
    }

    @Test
    fun `the window never exceeds half the bank`() {
        assertEquals(50, RecentCrownsPuzzles.windowFor(250))
        assertEquals(2, RecentCrownsPuzzles.windowFor(5))
        assertEquals(0, RecentCrownsPuzzles.windowFor(1))
    }

    @Test
    fun `a small bank still never repeats inside its window`() = runTest {
        val recent = RecentCrownsPuzzles(dataStore)
        val picks = List(60) { recent.pick(size = 7, count = 5, random = Random(it)) }

        picks.windowed(3).forEach { window -> assertEquals(3, window.toSet().size) }
    }

    @Test
    fun `recent picks are remembered across instances, like after a restart`() = runTest {
        val first = RecentCrownsPuzzles(dataStore).pick(size = 9, count = 2, random = Random(1))

        val second = RecentCrownsPuzzles(dataStore).pick(size = 9, count = 2, random = Random(2))

        assertTrue(first != second)
    }

    @Test
    fun `sizes are tracked separately`() = runTest {
        val recent = RecentCrownsPuzzles(dataStore)
        val picked7 = recent.pick(size = 7, count = 250, random = Random(1))
        val picked8 = recent.pick(size = 8, count = 250, random = Random(2))

        val stored = dataStore.data.first()

        assertEquals(picked7.toString(), stored[stringPreferencesKey("crowns_recent_7")])
        assertEquals(picked8.toString(), stored[stringPreferencesKey("crowns_recent_8")])
    }
}
