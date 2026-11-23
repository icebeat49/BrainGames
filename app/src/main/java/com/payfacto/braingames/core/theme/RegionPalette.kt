package com.payfacto.braingames.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Fill colours for puzzle regions. Light: Paul Tol's "light" qualitative scheme, chosen to
 * stay distinguishable with common colour-vision deficiencies, plus a lavender as 10th.
 * Dark: the same hues darkened so light marks (onSurface) stay readable on them.
 * Regions also get thick borders, so colour is never the only cue.
 */
@Immutable
data class RegionPalette(
    val fills: List<Color>,
    /** Pieces drawn on the regions (e.g. crowns): a fill and an outline chosen to contrast with every fill. */
    val pieceFill: Color,
    val pieceOutline: Color
) {
    fun fillFor(region: Int): Color = fills[region % fills.size]
}

val LightRegionPalette = RegionPalette(
    listOf(
        Color(0xFF77AADD), // light blue
        Color(0xFFEE8866), // orange
        Color(0xFFEEDD88), // light yellow
        Color(0xFFFFAABB), // pink
        Color(0xFF99DDFF), // light cyan
        Color(0xFF44BB99), // mint
        Color(0xFFBBCC33), // pear
        Color(0xFFAAAA00), // olive
        Color(0xFFDDDDDD), // pale grey
        Color(0xFFCCBBEE), // lavender
    ),
    // Light fills: a very dark piece with a white rim.
    pieceFill = Color(0xFF231C3A),
    pieceOutline = Color(0xFFFFFFFF)
)

val DarkRegionPalette = RegionPalette(
    listOf(
        Color(0xFF2E5578),
        Color(0xFF8A4330),
        Color(0xFF7A6B2A),
        Color(0xFF8A4458),
        Color(0xFF2F6B85),
        Color(0xFF1F6B57),
        Color(0xFF5C6619),
        Color(0xFF6B5200),
        Color(0xFF555555),
        Color(0xFF54467A),
    ),
    // Dark fills: a bright gold piece with a near-black rim.
    pieceFill = Color(0xFFFFD54F),
    pieceOutline = Color(0xFF141414)
)

val LocalRegionPalette = staticCompositionLocalOf { LightRegionPalette }
