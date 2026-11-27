package com.payfacto.braingames.feature.kakuro.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell
import kotlin.math.floor

/**
 * The Kakuro board: rounded tiles on a board background. Clues are amber tiles, cells to fill
 * are light tiles, and blocks are left empty. The selected cell is blue and the row and column
 * it belongs to are tinted, so it is clear which two sums it counts towards.
 */
@Composable
fun KakuroGrid(
    board: KakuroBoard,
    selectedCell: Pair<Int, Int>?,
    onCellTap: (Int, Int) -> Unit
) {
    if (board.rows == 0 || board.cols == 0) return
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE
    val colors = if (dark) DarkKakuroColors else LightKakuroColors
    val paints = remember { KakuroPaints() }
    val runCells = remember(board, selectedCell) {
        selectedCell?.let { (row, col) -> board.runCellsThrough(row, col) }.orEmpty()
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(board.cols.toFloat() / board.rows.toFloat())
            .pointerInput(board) {
                detectTapGestures { offset ->
                    val cellW = size.width / board.cols.toFloat()
                    val cellH = size.height / board.rows.toFloat()
                    val col = floor(offset.x / cellW).toInt().coerceIn(0, board.cols - 1)
                    val row = floor(offset.y / cellH).toInt().coerceIn(0, board.rows - 1)
                    onCellTap(row, col)
                }
            }
    ) {
        val cellSize = Size(size.width / board.cols, size.height / board.rows)
        val gap = cellSize.minDimension * TILE_GAP
        val tile = Tile(Size(cellSize.width - gap, cellSize.height - gap), CornerRadius(cellSize.minDimension * TILE_CORNER))
        paints.resize(tile.size.height)
        drawRoundRect(colors.board, cornerRadius = CornerRadius(cellSize.minDimension * BOARD_CORNER))
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val topLeft = Offset(c * cellSize.width + gap / 2, r * cellSize.height + gap / 2)
                when (val cell = board.cellAt(r, c)) {
                    is KakuroCell.Black -> Unit
                    is KakuroCell.Clue -> drawClue(cell, topLeft, tile, colors, paints.clue)
                    is KakuroCell.Input -> {
                        val background = when {
                            selectedCell == r to c -> colors.selected
                            (r to c) in runCells -> colors.run
                            else -> colors.cell
                        }
                        val digitColor = if (selectedCell == r to c) colors.selectedDigit else colors.digit
                        drawInput(cell, topLeft, tile, background, digitColor, paints.digit)
                    }
                }
            }
        }
    }
}

/** The size and corners of one tile, the same for every cell. */
private class Tile(val size: Size, val corner: CornerRadius)

/** Fixed colours, the same in every colour theme (like the Crowns regions), in a light and a dark set. */
private class KakuroColors(
    val board: Color,
    val clue: Color,
    val clueText: Color,
    val cell: Color,
    val digit: Color,
    val run: Color,
    val selected: Color,
    val selectedDigit: Color
)

private val LightKakuroColors = KakuroColors(
    board = Color(0xFFDDE3EC),
    clue = Color(0xFFF2A93B),
    clueText = Color(0xFF4A2A00),
    cell = Color(0xFFFFFFFF),
    digit = Color(0xFF1B4F9C),
    run = Color(0xFFCFE3FF),
    selected = Color(0xFF5B8DEF),
    selectedDigit = Color(0xFFFFFFFF)
)

private val DarkKakuroColors = KakuroColors(
    board = Color(0xFF141A26),
    clue = Color(0xFFE59A2F),
    clueText = Color(0xFF2B1700),
    cell = Color(0xFF2A3345),
    digit = Color(0xFF9CC3FF),
    run = Color(0xFF2F4A78),
    selected = Color(0xFF5B8DEF),
    selectedDigit = Color(0xFFFFFFFF)
)

/** Below this surface luminance the theme is dark (same rule as the Crowns regions). */
private const val DARK_SURFACE_LUMINANCE = 0.5f

/** Space between tiles, corner of each tile and corner of the board, as fractions of a cell. */
private const val TILE_GAP = 0.08f
private const val TILE_CORNER = 0.18f
private const val BOARD_CORNER = 0.22f

/** Text paints, made once and resized to the tile height on each draw. */
private class KakuroPaints {
    val digit = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }
    val clue = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }

    fun resize(tileHeight: Float) {
        digit.textSize = tileHeight * 0.6f
        clue.textSize = tileHeight * 0.3f
    }
}

/**
 * A clue tile, drawn the usual Kakuro way: a diagonal from the top-left corner to the bottom-right
 * one, the across sum in the top-right triangle (next to its row) and the down sum in the
 * bottom-left triangle (above its column), both clear of the line.
 */
private fun DrawScope.drawClue(cell: KakuroCell.Clue, topLeft: Offset, tile: Tile, colors: KakuroColors, paint: Paint) {
    drawRoundRect(colors.clue, topLeft, tile.size, tile.corner)
    val inset = tile.corner.x * 0.6f
    drawLine(
        color = colors.clueText.copy(alpha = 0.45f),
        start = topLeft + Offset(inset, inset),
        end = topLeft + Offset(tile.size.width - inset, tile.size.height - inset),
        strokeWidth = 1.5f
    )
    paint.color = colors.clueText.toArgb()
    // Digits are about 0.72 of the text size tall: half of that below the centre puts them on it.
    val baselineShift = paint.textSize * 0.36f
    if (cell.acrossSum > 0) {
        val centre = topLeft + Offset(tile.size.width * CLUE_FAR, tile.size.height * CLUE_NEAR)
        drawText(cell.acrossSum, centre.copy(y = centre.y + baselineShift), paint)
    }
    if (cell.downSum > 0) {
        val centre = topLeft + Offset(tile.size.width * CLUE_NEAR, tile.size.height * CLUE_FAR)
        drawText(cell.downSum, centre.copy(y = centre.y + baselineShift), paint)
    }
}

/** Where a sum is centred in its triangle, as a fraction of the tile: two-digit sums stay clear of the diagonal. */
private const val CLUE_NEAR = 0.30f
private const val CLUE_FAR = 0.70f

private fun DrawScope.drawInput(
    cell: KakuroCell.Input,
    topLeft: Offset,
    tile: Tile,
    background: Color,
    digitColor: Color,
    paint: Paint
) {
    drawRoundRect(background, topLeft, tile.size, tile.corner)
    if (cell.current != 0) {
        paint.color = digitColor.toArgb()
        val centre = topLeft + Offset(tile.size.width / 2f, tile.size.height / 2f)
        drawText(cell.current, centre.copy(y = centre.y + paint.textSize * 0.36f), paint)
    }
}

private fun DrawScope.drawText(value: Int, baseline: Offset, paint: Paint) {
    drawContext.canvas.nativeCanvas.drawText(value.toString(), baseline.x, baseline.y, paint)
}
