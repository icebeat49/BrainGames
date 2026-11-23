package com.payfacto.braingames.feature.kakuro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell
import kotlin.math.floor

@Composable
fun KakuroGrid(
    board: KakuroBoard,
    selectedCell: Pair<Int, Int>?,
    primaryColor: Color,
    onCellTap: (Int, Int) -> Unit
) {
    if (board.rows == 0 || board.cols == 0) return

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
        val cellW = size.width / board.cols.toFloat()
        val cellH = size.height / board.rows.toFloat()

        val textPaintLarge = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = cellH * 0.55f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val textPaintSmall = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = cellH * 0.28f
            isAntiAlias = true
        }

        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val left = c * cellW
                val top = r * cellH
                val cellSize = Size(cellW, cellH)

                when (val cell = board.cellAt(r, c)) {
                    is KakuroCell.Black -> {
                        drawRect(Color.DarkGray, Offset(left, top), cellSize)
                    }
                    is KakuroCell.Clue -> {
                        drawRect(Color.DarkGray, Offset(left, top), cellSize)
                        // diagonal line
                        drawLine(
                            color = Color.White,
                            start = Offset(left + 2f, top + cellH - 2f),
                            end = Offset(left + cellW - 2f, top + 2f),
                            strokeWidth = 2f
                        )
                        textPaintSmall.color = android.graphics.Color.WHITE
                        // across sum — bottom-left
                        if (cell.acrossSum > 0) {
                            drawContext.canvas.nativeCanvas.drawText(
                                cell.acrossSum.toString(),
                                left + cellW * 0.28f,
                                top + cellH * 0.85f,
                                textPaintSmall
                            )
                        }
                        // down sum — top-right
                        if (cell.downSum > 0) {
                            drawContext.canvas.nativeCanvas.drawText(
                                cell.downSum.toString(),
                                left + cellW * 0.72f,
                                top + cellH * 0.42f,
                                textPaintSmall
                            )
                        }
                    }
                    is KakuroCell.Input -> {
                        val isSelected = selectedCell?.first == r && selectedCell.second == c
                        val bgColor = when {
                            isSelected -> primaryColor.copy(alpha = 0.3f)
                            else -> Color.White
                        }
                        drawRect(bgColor, Offset(left, top), cellSize)
                        if (cell.current != 0) {
                            textPaintLarge.color = primaryColor.toArgb()
                            drawContext.canvas.nativeCanvas.drawText(
                                cell.current.toString(),
                                left + cellW / 2f,
                                top + cellH * 0.68f,
                                textPaintLarge
                            )
                        }
                    }
                }

                // cell border
                drawRect(
                    color = Color.Gray,
                    topLeft = Offset(left, top),
                    size = cellSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
            }
        }
    }
}

private fun Color.toArgb(): Int {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
