package com.payfacto.braingames.feature.sudoku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.payfacto.braingames.feature.sudoku.model.SudokuBoard
import kotlin.math.floor

@Composable
fun SudokuGrid(
    board: SudokuBoard,
    selectedCell: Int,
    primaryColor: Color,
    onSurface: Color,
    onCellTap: (Int) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellSize = size.width / 9f
                    val col = floor(offset.x / cellSize).toInt().coerceIn(0, 8)
                    val row = floor(offset.y / cellSize).toInt().coerceIn(0, 8)
                    onCellTap(row * 9 + col)
                }
            }
    ) {
        val cellSize = size.width / 9f
        drawGrid(board, selectedCell, primaryColor, onSurface, cellSize)
    }
}

private fun DrawScope.drawGrid(
    board: SudokuBoard,
    selectedCell: Int,
    primaryColor: Color,
    onSurface: Color,
    cellSize: Float
) {
    val selectedRow = if (selectedCell >= 0) selectedCell / 9 else -1
    val selectedCol = if (selectedCell >= 0) selectedCell % 9 else -1
    val selectedBox = if (selectedCell >= 0) (selectedRow / 3) * 3 + selectedCol / 3 else -1
    val selectedValue = if (selectedCell >= 0) board.current[selectedCell] else 0

    // Draw cell backgrounds
    for (row in 0 until 9) {
        for (col in 0 until 9) {
            val idx = row * 9 + col
            val box = (row / 3) * 3 + col / 3
            val bgColor = when {
                idx == selectedCell -> primaryColor.copy(alpha = 0.4f)
                selectedValue != 0 && board.current[idx] == selectedValue ->
                    primaryColor.copy(alpha = 0.2f)
                row == selectedRow || col == selectedCol || box == selectedBox ->
                    primaryColor.copy(alpha = 0.12f)
                else -> Color.Transparent
            }
            if (bgColor != Color.Transparent) {
                drawRect(
                    color = bgColor,
                    topLeft = Offset(col * cellSize, row * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }

            // Conflict highlight
            if (board.conflictsAt(idx)) {
                drawRect(
                    color = Color.Red.copy(alpha = 0.2f),
                    topLeft = Offset(col * cellSize, row * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }
    }

    // Draw numbers
    val textPaint = android.graphics.Paint().apply {
        textSize = cellSize * 0.55f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    for (row in 0 until 9) {
        for (col in 0 until 9) {
            val idx = row * 9 + col
            val value = board.current[idx]
            if (value != 0) {
                val isGiven = board.given[idx]
                val isConflict = board.conflictsAt(idx)
                textPaint.color = when {
                    isConflict -> android.graphics.Color.RED
                    isGiven    -> onSurface.toArgb()
                    else       -> primaryColor.toArgb()
                }
                textPaint.isFakeBoldText = isGiven
                drawContext.canvas.nativeCanvas.drawText(
                    value.toString(),
                    col * cellSize + cellSize / 2f,
                    row * cellSize + cellSize * 0.68f,
                    textPaint
                )
            }
        }
    }

    // Draw notes (candidate numbers)
    val notePaint = android.graphics.Paint().apply {
        textSize = cellSize * 0.25f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        color = onSurface.copy(alpha = 0.55f).toArgb()
    }
    val subCellSize = cellSize / 3f
    for (row in 0 until 9) {
        for (col in 0 until 9) {
            val idx = row * 9 + col
            val notesForCell = board.notes[idx]
            if (notesForCell.isNotEmpty() && board.current[idx] == 0) {
                for (n in notesForCell) {
                    val noteRow = (n - 1) / 3
                    val noteCol = (n - 1) % 3
                    val noteX = col * cellSize + (noteCol + 0.5f) * subCellSize
                    val noteY = row * cellSize + (noteRow + 0.5f) * subCellSize + notePaint.textSize * 0.38f
                    drawContext.canvas.nativeCanvas.drawText(n.toString(), noteX, noteY, notePaint)
                }
            }
        }
    }

    // Draw grid lines
    val thinStroke = 1.5f
    val thickStroke = 4f
    val lineColor = onSurface.copy(alpha = 0.25f)
    val boxColor = onSurface.copy(alpha = 0.7f)

    for (i in 0..9) {
        val pos = i * cellSize
        val stroke = if (i % 3 == 0) thickStroke else thinStroke
        val color = if (i % 3 == 0) boxColor else lineColor
        drawLine(color, Offset(0f, pos), Offset(size.width, pos), strokeWidth = stroke)
        drawLine(color, Offset(pos, 0f), Offset(pos, size.height), strokeWidth = stroke)
    }
}

private fun Color.toArgb(): Int {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
