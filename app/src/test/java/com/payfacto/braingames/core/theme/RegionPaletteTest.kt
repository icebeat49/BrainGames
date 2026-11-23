package com.payfacto.braingames.core.theme

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.engine.CrownsDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionPaletteTest {

    private val largestGrid = Difficulty.entries.maxOf(CrownsDifficulty::sizeFor)

    @Test
    fun `each palette gives every region of the largest grid its own colour`() {
        listOf(LightRegionPalette, DarkRegionPalette).forEach { palette ->
            val colours = (0 until largestGrid).map(palette::fillFor)
            assertEquals(largestGrid, colours.toSet().size)
        }
    }

    @Test
    fun `light and dark palettes have the same number of colours`() {
        assertEquals(LightRegionPalette.fills.size, DarkRegionPalette.fills.size)
        assertTrue(LightRegionPalette.fills.size >= largestGrid)
    }
}
