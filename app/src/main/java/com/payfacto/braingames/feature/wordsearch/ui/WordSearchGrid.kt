package com.payfacto.braingames.feature.wordsearch.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.payfacto.braingames.feature.wordsearch.model.WordEntry
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard
import kotlin.math.abs
import kotlin.math.floor

@Composable
fun WordSearchGrid(
    board: WordSearchBoard,
    hintCell: Pair<Int, Int>?,
    primaryColor: Color,
    onSurface: Color,
    revealHiddenWord: Boolean = false,
    onSelectionComplete: (List<Pair<Int, Int>>) -> Unit
) {
    var dragStart by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragCurrent by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val pulse by rememberInfiniteTransition(label = "hint").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(board.cols.toFloat() / board.rows.toFloat())
            .pointerInput(board) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val cellW = size.width / board.cols.toFloat()
                        val cellH = size.height / board.rows.toFloat()
                        val col = floor(offset.x / cellW).toInt().coerceIn(0, board.cols - 1)
                        val row = floor(offset.y / cellH).toInt().coerceIn(0, board.rows - 1)
                        dragStart = Pair(row, col)
                        dragCurrent = Pair(row, col)
                    },
                    onDrag = { change, _ ->
                        val cellW = size.width / board.cols.toFloat()
                        val cellH = size.height / board.rows.toFloat()
                        val col = floor(change.position.x / cellW).toInt().coerceIn(0, board.cols - 1)
                        val row = floor(change.position.y / cellH).toInt().coerceIn(0, board.rows - 1)
                        dragCurrent = Pair(row, col)
                    },
                    onDragEnd = {
                        val start = dragStart
                        val end = dragCurrent
                        if (start != null && end != null) {
                            onSelectionComplete(getCellsOnLine(start, end))
                        }
                        dragStart = null
                        dragCurrent = null
                    },
                    onDragCancel = {
                        dragStart = null
                        dragCurrent = null
                    }
                )
            }
    ) {
        val cellW = size.width / board.cols.toFloat()
        val cellH = size.height / board.rows.toFloat()

        // Hidden word reveal highlight (golden)
        if (revealHiddenWord) {
            board.hiddenWordCells.forEach { (r, c) ->
                drawRect(
                    color = Color(1f, 0.8f, 0f, 0.45f),
                    topLeft = Offset(c * cellW, r * cellH),
                    size = Size(cellW, cellH)
                )
            }
        }

        // Found word highlights
        board.words.filter { it.found }.forEach { entry ->
            drawWordHighlight(entry, cellW, cellH, primaryColor.copy(alpha = 0.35f))
        }

        // Current drag selection
        if (dragStart != null && dragCurrent != null) {
            getCellsOnLine(dragStart!!, dragCurrent!!).forEach { (r, c) ->
                drawRect(
                    color = primaryColor.copy(alpha = 0.25f),
                    topLeft = Offset(c * cellW, r * cellH),
                    size = Size(cellW, cellH)
                )
            }
        }

        // Hint pulse ring
        if (hintCell != null) {
            val (hr, hc) = hintCell
            drawCircle(
                color = Color.Yellow.copy(alpha = pulse),
                radius = cellW * 0.45f,
                center = Offset(hc * cellW + cellW / 2f, hr * cellH + cellH / 2f),
                style = Stroke(width = 4f)
            )
        }

        // Letters
        val textPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = minOf(cellW, cellH) * 0.6f
            isAntiAlias = true
            isFakeBoldText = true
        }
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val ch = board.grid[r][c]
                val isHiddenCell = revealHiddenWord && board.hiddenWordCells.contains(Pair(r, c))
                textPaint.color = when {
                    isHiddenCell -> primaryColor.toArgb()
                    else -> onSurface.toArgb()
                }
                drawContext.canvas.nativeCanvas.drawText(
                    ch.toString(),
                    c * cellW + cellW / 2f,
                    r * cellH + cellH * 0.68f,
                    textPaint
                )
            }
        }

        // Grid lines
        val lineColor = onSurface.copy(alpha = 0.18f)
        for (i in 0..board.cols) {
            drawLine(lineColor, Offset(i * cellW, 0f), Offset(i * cellW, size.height), strokeWidth = 1f)
        }
        for (i in 0..board.rows) {
            drawLine(lineColor, Offset(0f, i * cellH), Offset(size.width, i * cellH), strokeWidth = 1f)
        }
    }
}

private fun DrawScope.drawWordHighlight(entry: WordEntry, cellW: Float, cellH: Float, color: Color) {
    for (i in entry.word.indices) {
        val r = entry.startRow + entry.direction.dRow * i
        val c = entry.startCol + entry.direction.dCol * i
        drawRect(color = color, topLeft = Offset(c * cellW, r * cellH), size = Size(cellW, cellH))
    }
}

private fun getCellsOnLine(start: Pair<Int, Int>, end: Pair<Int, Int>): List<Pair<Int, Int>> {
    val dRow = end.first - start.first
    val dCol = end.second - start.second
    if (dRow == 0 && dCol == 0) return listOf(start)
    val normRow = if (dRow > 0) 1 else if (dRow < 0) -1 else 0
    val normCol = if (dCol > 0) 1 else if (dCol < 0) -1 else 0
    val steps = maxOf(abs(dRow), abs(dCol))
    return (0..steps).map { i -> Pair(start.first + normRow * i, start.second + normCol * i) }
}
