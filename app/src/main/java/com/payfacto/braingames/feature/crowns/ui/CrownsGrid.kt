package com.payfacto.braingames.feature.crowns.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payfacto.braingames.R
import com.payfacto.braingames.core.theme.LocalRegionPalette
import com.payfacto.braingames.core.theme.RegionPalette
import com.payfacto.braingames.core.ui.grid.GridGeometry
import com.payfacto.braingames.core.ui.grid.GridPoint
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import com.payfacto.braingames.feature.crowns.model.CellMark

/** What the grid highlights besides the marks. */
data class CrownsGridHighlights(
    val conflictCells: Set<Int> = emptySet(),
    val errorCells: Set<Int> = emptySet(),
    val hintCells: Set<Int> = emptySet(),
    val hintAreaCells: Set<Int> = emptySet()
)

/** Input callbacks; the rules live in CrownsGestures behind the ViewModel. */
class CrownsGridInput(
    val onTap: (cell: Int, downMillis: Long, upMillis: Long, doubleTapTimeoutMillis: Long) -> Unit,
    val onDragStart: (GridPoint) -> Unit,
    val onDragMove: (GridPoint) -> Unit,
    val onDragEnd: () -> Unit,
    /** Accessibility: set one cell directly. */
    val onSetMark: (cell: Int, mark: CellMark) -> Unit
)

@Composable
fun CrownsGrid(
    puzzle: CrownsPuzzle,
    board: CrownsBoard,
    highlights: CrownsGridHighlights,
    isQuestionMode: Boolean,
    input: CrownsGridInput,
    modifier: Modifier = Modifier
) {
    val currentInput by rememberUpdatedState(input)
    val n = puzzle.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(n) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val geometry = GridGeometry(size.width.toFloat(), size.height.toFloat(), n, n)
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            if (dragging) {
                                currentInput.onDragEnd()
                            } else if (change != null) {
                                geometry.cellAt(down.position.x, down.position.y)?.let { cell ->
                                    currentInput.onTap(
                                        cell, down.uptimeMillis, change.uptimeMillis,
                                        viewConfiguration.doubleTapTimeoutMillis
                                    )
                                }
                            }
                            change?.consume()
                            break
                        }
                        if (!dragging && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                            dragging = true
                            currentInput.onDragStart(geometry.toGridPoint(down.position.x, down.position.y))
                        }
                        if (dragging) {
                            currentInput.onDragMove(geometry.toGridPoint(change.position.x, change.position.y))
                            change.consume()
                        }
                    }
                }
            }
    ) {
        GridCanvas(puzzle, board, highlights, LocalRegionPalette.current)
        AccessibilityCells(puzzle, board, highlights, isQuestionMode, currentInput)
    }
}

@Composable
private fun GridCanvas(
    puzzle: CrownsPuzzle,
    board: CrownsBoard,
    highlights: CrownsGridHighlights,
    palette: RegionPalette
) {
    val colors = MaterialTheme.colorScheme
    val crownPainter = rememberVectorPainter(
        remember(palette) { crownVector(fill = palette.pieceFill, outline = palette.pieceOutline) }
    )
    val flaggedCrownPainter = rememberVectorPainter(
        remember(palette, colors.error) { crownVector(fill = colors.error, outline = palette.pieceOutline) }
    )
    val textMeasurer = rememberTextMeasurer()
    val questionStyle = TextStyle(color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val n = puzzle.size
        val geometry = GridGeometry(size.width, size.height, n, n)
        val cellSize = Size(geometry.cellWidth, geometry.cellHeight)
        fun topLeft(cell: Int) = Offset(geometry.cellLeft(cell), geometry.cellTop(cell))

        for (cell in 0 until puzzle.cellCount) {
            drawRect(palette.fillFor(puzzle.regionOf(cell)), topLeft(cell), cellSize)
            when (cell) {
                in highlights.errorCells -> drawRect(colors.error.copy(alpha = 0.35f), topLeft(cell), cellSize)
                in highlights.hintCells -> drawRect(colors.primary.copy(alpha = 0.35f), topLeft(cell), cellSize)
                in highlights.hintAreaCells -> drawRect(colors.primary.copy(alpha = 0.15f), topLeft(cell), cellSize)
            }
        }

        for (cell in 0 until puzzle.cellCount) {
            val origin = topLeft(cell)
            val isFlagged = cell in highlights.conflictCells || cell in highlights.errorCells
            when (board[cell]) {
                CellMark.EMPTY -> Unit
                CellMark.CROSS -> drawCross(origin, cellSize, colors.onSurface.copy(alpha = 0.75f))
                CellMark.QUESTION -> {
                    val layout = textMeasurer.measure("?", questionStyle.copy(fontSize = (cellSize.height * 0.45f).toSp()))
                    drawText(
                        layout,
                        topLeft = origin + Offset(
                            (cellSize.width - layout.size.width) / 2f,
                            (cellSize.height - layout.size.height) / 2f
                        )
                    )
                }
                CellMark.CROWN -> {
                    val iconSize = cellSize.minDimension * 0.78f
                    val painter = if (isFlagged) flaggedCrownPainter else crownPainter
                    translate(
                        left = origin.x + (cellSize.width - iconSize) / 2f,
                        top = origin.y + (cellSize.height - iconSize) / 2f
                    ) {
                        with(painter) { draw(Size(iconSize, iconSize)) }
                    }
                }
            }
            if (cell in highlights.conflictCells) {
                val inset = cellSize.minDimension * 0.06f
                drawRect(
                    color = colors.error,
                    topLeft = origin + Offset(inset, inset),
                    size = Size(cellSize.width - 2 * inset, cellSize.height - 2 * inset),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        drawRegionLines(puzzle, geometry, colors.onSurface)
    }
}

private fun DrawScope.drawCross(origin: Offset, cell: Size, color: Color) {
    val inset = cell.minDimension * 0.3f
    val stroke = cell.minDimension * 0.07f
    drawLine(color, origin + Offset(inset, inset), origin + Offset(cell.width - inset, cell.height - inset), stroke)
    drawLine(color, origin + Offset(cell.width - inset, inset), origin + Offset(inset, cell.height - inset), stroke)
}

/** Thin lines inside a region, thick lines between regions and around the grid. */
private fun DrawScope.drawRegionLines(puzzle: CrownsPuzzle, geometry: GridGeometry, color: Color) {
    val n = puzzle.size
    val thin = 1.dp.toPx()
    val thick = 3.dp.toPx()
    val thinColor = color.copy(alpha = 0.25f)
    val edges = mutableListOf<Triple<Offset, Offset, Boolean>>() // start, end, thick?

    for (row in 0 until n) for (col in 0 until n) {
        val cell = puzzle.cellAt(row, col)
        val x = geometry.cellLeft(cell)
        val y = geometry.cellTop(cell)
        if (col < n - 1) {
            val border = puzzle.regionOf(cell) != puzzle.regionOf(cell + 1)
            edges += Triple(Offset(x + geometry.cellWidth, y), Offset(x + geometry.cellWidth, y + geometry.cellHeight), border)
        }
        if (row < n - 1) {
            val border = puzzle.regionOf(cell) != puzzle.regionOf(cell + n)
            edges += Triple(Offset(x, y + geometry.cellHeight), Offset(x + geometry.cellWidth, y + geometry.cellHeight), border)
        }
    }
    edges.filterNot { it.third }.forEach { (start, end) -> drawLine(thinColor, start, end, thin) }
    edges.filter { it.third }.forEach { (start, end) -> drawLine(color, start, end, thick) }
    drawRect(color, style = Stroke(width = thick))
}

/** Invisible cells on top of the canvas carrying TalkBack descriptions and actions. */
@Composable
private fun AccessibilityCells(
    puzzle: CrownsPuzzle,
    board: CrownsBoard,
    highlights: CrownsGridHighlights,
    isQuestionMode: Boolean,
    input: CrownsGridInput
) {
    val markLabel = stringResource(R.string.crowns_action_mark)
    val placeLabel = stringResource(R.string.crowns_action_place)
    val clearLabel = stringResource(R.string.action_clear)
    val markNames = mapOf(
        CellMark.EMPTY to stringResource(R.string.crowns_mark_empty),
        CellMark.CROSS to stringResource(R.string.crowns_mark_cross),
        CellMark.QUESTION to stringResource(R.string.crowns_mark_question),
        CellMark.CROWN to stringResource(R.string.crowns_mark_crown)
    )
    val conflictSuffix = stringResource(R.string.crowns_cell_conflict)
    val wrongSuffix = stringResource(R.string.crowns_cell_wrong)

    Column(Modifier.fillMaxSize()) {
        for (row in 0 until puzzle.size) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0 until puzzle.size) {
                    val cell = puzzle.cellAt(row, col)
                    val mark = board[cell]
                    val description = stringResource(
                        R.string.crowns_cell_description,
                        row + 1, col + 1, puzzle.regionOf(cell) + 1, markNames.getValue(mark)
                    ) + (if (cell in highlights.conflictCells) conflictSuffix else "") +
                        (if (cell in highlights.errorCells) wrongSuffix else "")
                    val tapMark = when {
                        mark != CellMark.EMPTY -> CellMark.EMPTY
                        isQuestionMode -> CellMark.QUESTION
                        else -> CellMark.CROSS
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics {
                                contentDescription = description
                                onClick(label = markLabel) { input.onSetMark(cell, tapMark); true }
                                customActions = listOf(
                                    CustomAccessibilityAction(placeLabel) { input.onSetMark(cell, CellMark.CROWN); true },
                                    CustomAccessibilityAction(clearLabel) { input.onSetMark(cell, CellMark.EMPTY); true }
                                )
                            }
                    )
                }
            }
        }
    }
}
