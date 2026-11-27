package com.payfacto.braingames.core.theme

import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.feature.crowns.engine.CrownsDifficulty
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionPaletteTest {

    private val largestGrid = GameType.CROWNS.levels.maxOf(CrownsDifficulty::sizeFor)

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

    @Test
    fun `every app theme gets the region palette that matches its own darkness`() {
        allSchemes().forEach { (label, scheme) ->
            val expected = if (scheme.surface.luminance() < 0.5f) DarkRegionPalette else LightRegionPalette
            assertEquals(label, expected, regionPaletteFor(scheme))
        }
    }

    @Test
    fun `marks and crowns contrast with every region colour in every theme`() {
        allSchemes().forEach { (label, scheme) ->
            val palette = regionPaletteFor(scheme)
            palette.fills.take(largestGrid).forEachIndexed { index, fill ->
                assertTrue("$label region $index: marks", contrast(palette.mark, fill) >= MIN_GRAPHIC_CONTRAST)
                assertTrue("$label region $index: crown", contrast(palette.pieceFill, fill) >= MIN_GRAPHIC_CONTRAST)
            }
            assertTrue("$label: crown rim", contrast(palette.pieceFill, palette.pieceOutline) >= MIN_GRAPHIC_CONTRAST)
        }
    }

    private fun allSchemes(): List<Pair<String, ColorScheme>> =
        AppColorTheme.entries.flatMap { theme ->
            listOf(false, true).map { dark -> "$theme dark=$dark" to appColorScheme(theme, dark) }
        }

    /** WCAG contrast ratio. */
    private fun contrast(a: Color, b: Color): Float {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05f) / (dark + 0.05f)
    }

    private companion object {
        /** WCAG 2.1 minimum for graphical objects (1.4.11). */
        const val MIN_GRAPHIC_CONTRAST = 3f
    }
}
