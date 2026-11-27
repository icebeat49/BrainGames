package com.payfacto.braingames.feature.home

import androidx.compose.ui.geometry.Offset
import com.payfacto.braingames.core.data.GameOrderPreferences
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.model.GameType.CROWNS
import com.payfacto.braingames.core.model.GameType.KAKURO
import com.payfacto.braingames.core.model.GameType.SUDOKU
import com.payfacto.braingames.core.model.GameType.WORD_SEARCH
import com.payfacto.braingames.testutil.FakePreferencesDataStore
import com.payfacto.braingames.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeOrderTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val preferences = GameOrderPreferences(FakePreferencesDataStore())

    @Test
    fun `no saved order is the default order`() {
        assertEquals(GameType.entries, GameOrderPreferences.orderFrom(null))
    }

    @Test
    fun `a saved order skips unknown and repeated names and appends games it does not mention`() {
        assertEquals(
            listOf(CROWNS, SUDOKU, KAKURO, WORD_SEARCH),
            GameOrderPreferences.orderFrom("CROWNS,MAHJONG,CROWNS,SUDOKU")
        )
    }

    @Test
    fun `reordering saves the new order, hidden games kept after the shown ones`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = HomeViewModel(preferences) { it != WORD_SEARCH }

        viewModel.reorder(listOf(CROWNS, SUDOKU, KAKURO))
        advanceUntilIdle()

        assertEquals(listOf(CROWNS, SUDOKU, KAKURO), viewModel.games.value)
        assertEquals(listOf(CROWNS, SUDOKU, KAKURO, WORD_SEARCH), preferences.order.first())
    }

    @Test
    fun `move shifts a game one place and stops at the ends`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = HomeViewModel(preferences) { true }

        viewModel.move(KAKURO, -1)
        advanceUntilIdle()
        assertEquals(listOf(KAKURO, SUDOKU, WORD_SEARCH, CROWNS), viewModel.games.value)

        viewModel.move(KAKURO, -1)
        advanceUntilIdle()
        assertEquals("already first", listOf(KAKURO, SUDOKU, WORD_SEARCH, CROWNS), viewModel.games.value)
    }

    @Test
    fun `moved puts an item at the new index and shifts the others`() {
        assertEquals(listOf("b", "c", "a", "d"), listOf("a", "b", "c", "d").moved("a", 2))
        assertEquals(listOf("d", "a", "b", "c"), listOf("a", "b", "c", "d").moved("d", 0))
    }

    @Test
    fun `grid slots - two columns, touches in a gap hit nothing, a drag lands on the nearest slot`() {
        // Cells 100 wide, 80 tall, 10 apart: slot 3 starts at (110, 90).
        val slots = GridSlots(cellWidth = 100f, cellHeight = 80f, gap = 10f, count = 4)

        assertEquals(Offset(110f, 90f), slots.origin(3))
        assertEquals(170f, slots.height)
        assertEquals(3, slots.slotAt(Offset(150f, 120f)))
        assertNull("in the gap between columns", slots.slotAt(Offset(105f, 40f)))
        assertEquals(2, slots.nearestSlot(Offset(60f, 150f)))
    }
}
