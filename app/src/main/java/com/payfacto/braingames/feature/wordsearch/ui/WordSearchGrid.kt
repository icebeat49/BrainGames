package com.payfacto.braingames.feature.wordsearch.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor

@Composable
fun WordSearchGrid(
    board: WordSearchBoard,
    hintCell: Pair<Int, Int>?,
    primaryColor: Color,
    onSurface: Color,
    /** Cells lit up in gold: the leftover letters, once there is something to read in them. */
    leftoverCells: List<Pair<Int, Int>> = emptyList(),
    modifier: Modifier = Modifier,
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

    val leftoverSet = remember(leftoverCells) { leftoverCells.toSet() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(board.cols.toFloat() / board.rows.toFloat())
            .pointerInput(board) {
                fun cellAt(position: Offset) = Pair(
                    floor(position.y / (size.height / board.rows.toFloat())).toInt().coerceIn(0, board.rows - 1),
                    floor(position.x / (size.width / board.cols.toFloat())).toInt().coerceIn(0, board.cols - 1)
                )
                // The selection starts where the finger touches down, not where it has moved past
                // the touch slop (~8dp): on 18×18 cells that would already be the next letter.
                awaitEachGesture {
                    val down = awaitFirstDown()
                    dragStart = cellAt(down.position)
                    dragCurrent = dragStart
                    val completed = drag(down.id) { change ->
                        change.consume()
                        dragCurrent = cellAt(change.position)
                    }
                    val start = dragStart
                    val end = dragCurrent
                    if (completed && start != null && end != null) {
                        onSelectionComplete(getCellsOnLine(start, end, board.rows, board.cols))
                    }
                    dragStart = null
                    dragCurrent = null
                }
            }
    ) {
        val cellW = size.width / board.cols.toFloat()
        val cellH = size.height / board.rows.toFloat()

        // Leftover letters (golden)
        leftoverCells.forEach { (r, c) ->
            drawRect(
                color = Color(1f, 0.8f, 0f, 0.45f),
                topLeft = Offset(c * cellW, r * cellH),
                size = Size(cellW, cellH)
            )
        }

        // Lines: found words in Classic, the player's pencil lines in Hardcore
        board.marks.forEach { cells ->
            cells.forEach { (r, c) ->
                drawRect(
                    color = primaryColor.copy(alpha = 0.35f),
                    topLeft = Offset(c * cellW, r * cellH),
                    size = Size(cellW, cellH)
                )
            }
        }

        // Current drag selection
        if (dragStart != null && dragCurrent != null) {
            getCellsOnLine(dragStart!!, dragCurrent!!, board.rows, board.cols).forEach { (r, c) ->
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
                val isHiddenCell = leftoverSet.contains(Pair(r, c))
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

/**
 * The cells from [start] towards [end], snapped to the nearest of the eight directions and cut
 * short at the grid edge, so a slanted drag still selects a straight line inside the grid.
 */
internal fun getCellsOnLine(start: Pair<Int, Int>, end: Pair<Int, Int>, rows: Int, cols: Int): List<Pair<Int, Int>> {
    val dRow = end.first - start.first
    val dCol = end.second - start.second
    if (dRow == 0 && dCol == 0) return listOf(start)
    val octant = Math.floorMod(Math.round(atan2(dRow.toDouble(), dCol.toDouble()) / (PI / 4)).toInt(), 8)
    val (stepRow, stepCol) = OCTANT_STEPS[octant]
    var steps = maxOf(abs(dRow), abs(dCol))
    while (steps > 0 && (start.first + stepRow * steps !in 0 until rows || start.second + stepCol * steps !in 0 until cols)) steps--
    return (0..steps).map { i -> Pair(start.first + stepRow * i, start.second + stepCol * i) }
}

/** (row, col) step per 45° sector of atan2(row, col): right, then clockwise on screen. */
private val OCTANT_STEPS = listOf(0 to 1, 1 to 1, 1 to 0, 1 to -1, 0 to -1, -1 to -1, -1 to 0, -1 to 1)
