package com.payfacto.braingames.feature.kakuro.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell
import kotlin.math.floor

@Composable
fun KakuroGrid(
    board: KakuroBoard,
    selectedCell: Pair<Int, Int>?,
    onCellTap: (Int, Int) -> Unit
) {
    if (board.rows == 0 || board.cols == 0) return
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(colorScheme) { KakuroGridColors.from(colorScheme) }
    val paints = remember { KakuroPaints() }

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
        paints.resize(cellSize.height)
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val topLeft = Offset(c * cellSize.width, r * cellSize.height)
                when (val cell = board.cellAt(r, c)) {
                    is KakuroCell.Black -> drawRect(colors.block, topLeft, cellSize)
                    is KakuroCell.Clue -> drawClue(cell, topLeft, cellSize, colors, paints.clue)
                    is KakuroCell.Input -> {
                        val selected = selectedCell?.first == r && selectedCell.second == c
                        drawInput(cell, selected, topLeft, cellSize, colors, paints.digit)
                    }
                }
                drawRect(colors.gridLine, topLeft, cellSize, style = Stroke(width = 1f))
            }
        }
    }
}

/** Grid colours taken from the theme, so the grid follows light and dark themes. */
private class KakuroGridColors(
    val block: Color,
    val clueText: Color,
    val cell: Color,
    val selected: Color,
    val digit: Color,
    val gridLine: Color
) {
    companion object {
        /** Below this surface luminance the theme is dark (same rule as the region palette). */
        private const val DARK_SURFACE_LUMINANCE = 0.5f
        private const val LIGHT_BLOCK_SHADE = 0.75f
        private const val DARK_BLOCK_SHADE = 0.22f

        fun from(scheme: ColorScheme): KakuroGridColors {
            val dark = scheme.surface.luminance() < DARK_SURFACE_LUMINANCE
            val shade = if (dark) DARK_BLOCK_SHADE else LIGHT_BLOCK_SHADE
            return KakuroGridColors(
                block = lerp(scheme.surface, scheme.onSurface, shade),
                clueText = if (dark) scheme.onSurface else scheme.surface,
                cell = scheme.surface,
                selected = scheme.primary.copy(alpha = 0.3f).compositeOn(scheme.surface),
                digit = scheme.primary,
                gridLine = scheme.onSurface.copy(alpha = 0.35f)
            )
        }

        private fun Color.compositeOn(background: Color): Color = lerp(background, copy(alpha = 1f), alpha)
    }
}

/** Text paints, made once and resized to the cell height on each draw. */
private class KakuroPaints {
    val digit = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }
    val clue = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    fun resize(cellHeight: Float) {
        digit.textSize = cellHeight * 0.55f
        clue.textSize = cellHeight * 0.28f
    }
}

/**
 * A clue cell, drawn the usual Kakuro way: a diagonal from the top-left corner to the bottom-right
 * one, the across sum in the top-right triangle (next to its row) and the down sum in the
 * bottom-left triangle (above its column), both clear of the line.
 */
private fun DrawScope.drawClue(cell: KakuroCell.Clue, topLeft: Offset, cellSize: Size, colors: KakuroGridColors, paint: Paint) {
    drawRect(colors.block, topLeft, cellSize)
    drawLine(
        color = colors.clueText.copy(alpha = 0.6f),
        start = Offset(topLeft.x + 2f, topLeft.y + 2f),
        end = Offset(topLeft.x + cellSize.width - 2f, topLeft.y + cellSize.height - 2f),
        strokeWidth = 2f
    )
    paint.color = colors.clueText.toArgb()
    // Digits are about 0.72 of the text size tall: half of that below the centre puts them on it.
    val baselineShift = paint.textSize * 0.36f
    if (cell.acrossSum > 0) {
        val centre = Offset(topLeft.x + cellSize.width * CLUE_FAR, topLeft.y + cellSize.height * CLUE_NEAR)
        drawText(cell.acrossSum, centre.copy(y = centre.y + baselineShift), paint)
    }
    if (cell.downSum > 0) {
        val centre = Offset(topLeft.x + cellSize.width * CLUE_NEAR, topLeft.y + cellSize.height * CLUE_FAR)
        drawText(cell.downSum, centre.copy(y = centre.y + baselineShift), paint)
    }
}

/** Where a sum is centred in its triangle, as a fraction of the cell: two-digit sums stay clear of the diagonal. */
private const val CLUE_NEAR = 0.30f
private const val CLUE_FAR = 0.70f

private fun DrawScope.drawInput(
    cell: KakuroCell.Input,
    selected: Boolean,
    topLeft: Offset,
    cellSize: Size,
    colors: KakuroGridColors,
    paint: Paint
) {
    drawRect(if (selected) colors.selected else colors.cell, topLeft, cellSize)
    if (cell.current != 0) {
        paint.color = colors.digit.toArgb()
        drawText(cell.current, Offset(topLeft.x + cellSize.width / 2f, topLeft.y + cellSize.height * 0.68f), paint)
    }
}

private fun DrawScope.drawText(value: Int, baseline: Offset, paint: Paint) {
    drawContext.canvas.nativeCanvas.drawText(value.toString(), baseline.x, baseline.y, paint)
}
