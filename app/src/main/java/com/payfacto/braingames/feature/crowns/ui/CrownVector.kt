package com.payfacto.braingames.feature.crowns.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The game's crown: an original, deliberately bold shape so it stays readable in a 5×5 to
 * 9×9 cell on a small phone. Three points topped with round jewels over a body, and a
 * separate band below. Drawn on a 24×24 viewport.
 */
private const val CROWN_PATH =
    // Body: left point, valley, centre point, valley, right point, down to the base.
    "M3,8.5 L7.5,12.5 L12,5.5 L16.5,12.5 L21,8.5 L19,17 L5,17 Z " +
        // Jewels on the three points.
        "M3,6.2 m-2,0 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z " +
        "M12,3.6 m-2,0 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z " +
        "M21,6.2 m-2,0 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z " +
        // Band.
        "M5,18.6 L19,18.6 L19,21.4 L5,21.4 Z"

private const val VIEWPORT = 24f
private const val OUTLINE_WIDTH = 1.6f

/**
 * A two-tone crown for the board. Colours come from the theme (RegionPalette.pieceFill /
 * pieceOutline), so the same shape works on every region colour in light and dark mode.
 */
fun crownVector(fill: Color, outline: Color): ImageVector =
    ImageVector.Builder(
        name = "Crown",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT
    ).addPath(
        pathData = addPathNodes(CROWN_PATH),
        fill = SolidColor(fill),
        stroke = SolidColor(outline),
        strokeLineWidth = OUTLINE_WIDTH,
        strokeLineJoin = StrokeJoin.Round
    ).build()

/** Single-colour crown for icons (home card, catalog); Icon() tints it with the content colour. */
val CrownIcon: ImageVector = ImageVector.Builder(
    name = "CrownIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = VIEWPORT,
    viewportHeight = VIEWPORT
).addPath(
    pathData = addPathNodes(CROWN_PATH),
    fill = SolidColor(Color.Black)
).build()
