package com.payfacto.braingames.feature.wordsearch

import com.payfacto.braingames.feature.wordsearch.ui.getCellsOnLine
import org.junit.Assert.assertEquals
import org.junit.Test

class WordSearchSelectionTest {

    @Test
    fun `straight and diagonal drags select every cell between the ends`() {
        assertEquals(listOf(2 to 1, 2 to 2, 2 to 3), getCellsOnLine(2 to 1, 2 to 3, SIZE, SIZE))
        assertEquals(listOf(5 to 5, 4 to 4, 3 to 3), getCellsOnLine(5 to 5, 3 to 3, SIZE, SIZE))
        assertEquals(listOf(0 to 4, 1 to 3, 2 to 2), getCellsOnLine(0 to 4, 2 to 2, SIZE, SIZE))
    }

    @Test
    fun `a slanted drag snaps to the nearest of the eight directions`() {
        // 1 down, 5 across is closer to "right" than to "down-right".
        assertEquals((0..5).map { 3 to it }, getCellsOnLine(3 to 0, 4 to 5, SIZE, SIZE))
        // 4 down, 5 across is closer to the diagonal.
        assertEquals((0..5).map { it to it }, getCellsOnLine(0 to 0, 4 to 5, SIZE, SIZE))
    }

    @Test
    fun `a snapped line never leaves the grid`() {
        // 2 down, 3 across snaps to down-right; 3 steps from row 10 of 12 would reach row 13.
        assertEquals(listOf(10 to 0, 11 to 1), getCellsOnLine(10 to 0, 12 to 3, SIZE, SIZE))
    }

    @Test
    fun `a tap selects its single cell`() {
        assertEquals(listOf(4 to 4), getCellsOnLine(4 to 4, 4 to 4, SIZE, SIZE))
    }

    private companion object {
        const val SIZE = 12
    }
}
