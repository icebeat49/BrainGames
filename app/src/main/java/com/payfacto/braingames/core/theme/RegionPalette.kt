package com.payfacto.braingames.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colours for region puzzles (Crowns): a board, one vivid fill per region up to 12 (the largest
 * grid), and the marks and pieces drawn on them. The fills are mid-tones chosen to be told apart
 * side by side, the same in light and dark mode; only the board behind them changes.
 */
@Immutable
data class RegionPalette(
    val board: Color,
    val fills: List<Color>,
    /** ✕ and ? marks, dark enough to read on every fill. */
    val mark: Color,
    /** Pieces drawn on the regions (e.g. crowns): a fill and an outline chosen to contrast with every fill. */
    val pieceFill: Color,
    val pieceOutline: Color
) {
    fun fillFor(region: Int): Color = fills[region % fills.size]
}

private val VividRegionFills = listOf(
    Color(0xFF7FB2EE), // sky blue
    Color(0xFFF0A63A), // amber
    Color(0xFFD8BC3C), // mustard
    Color(0xFF93BE84), // sage
    Color(0xFF5584C8), // cobalt
    Color(0xFFC47A4B), // rust
    Color(0xFFDE6A5E), // coral
    Color(0xFFE096B0), // rose
    Color(0xFF9387E0), // violet
    Color(0xFF8792A3), // slate
    Color(0xFF5DB5B1), // teal
    Color(0xFFB9D44E), // lime (12th, for 12×12 grids)
)

private val RegionMark = Color(0xFF1B2230)

val LightRegionPalette = RegionPalette(
    board = Color(0xFFDDE3EC),
    fills = VividRegionFills,
    mark = RegionMark,
    // A very dark crown with a white rim, readable on every mid-tone fill.
    pieceFill = Color(0xFF231C3A),
    pieceOutline = Color(0xFFFFFFFF)
)

val DarkRegionPalette = LightRegionPalette.copy(board = Color(0xFF141A26))

val LocalRegionPalette = staticCompositionLocalOf { LightRegionPalette }
