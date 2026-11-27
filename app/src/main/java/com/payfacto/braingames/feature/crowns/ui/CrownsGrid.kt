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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.payfacto.braingames.core.motion.rememberAnimationsEnabled
import com.payfacto.braingames.core.theme.LocalRegionPalette
import com.payfacto.braingames.core.theme.RegionPalette
import com.payfacto.braingames.core.ui.grid.GridGeometry
import com.payfacto.braingames.core.ui.grid.GridPoint
import com.payfacto.braingames.feature.crowns.input.TapEvent
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
    val onTap: (TapEvent) -> Unit,
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
    modifier: Modifier = Modifier,
    /** 0..1: how strongly the whole board glows (win celebration). */
    winGlow: () -> Float = { 0f }
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
                                // The release can be past the last move sample on a fast swipe:
                                // cover that last stretch before ending the drag.
                                change?.let { currentInput.onDragMove(geometry.toGridPoint(it.position.x, it.position.y)) }
                                currentInput.onDragEnd()
                            } else if (change != null) {
                                currentInput.onTap(
                                    TapEvent(
                                        point = geometry.toGridPoint(down.position.x, down.position.y),
                                        downTimeMillis = down.uptimeMillis,
                                        upTimeMillis = change.uptimeMillis,
                                        doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis,
                                        snapRadiusCells = viewConfiguration.touchSlop / geometry.cellWidth
                                    )
                                )
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
        GridCanvas(puzzle, board, highlights, LocalRegionPalette.current, winGlow)
        AccessibilityCells(puzzle, board, highlights, isQuestionMode, currentInput)
    }
}

/**
 * The board as a picture, drawn by the same renderer as the game, without input or
 * animations (How to play). TalkBack reads [description] instead of the cells.
 */
@Composable
internal fun CrownsBoardPicture(
    puzzle: CrownsPuzzle,
    board: CrownsBoard,
    description: String,
    modifier: Modifier = Modifier,
    highlights: CrownsGridHighlights = CrownsGridHighlights()
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clearAndSetSemantics { contentDescription = description }
    ) {
        GridCanvas(puzzle, board, highlights, LocalRegionPalette.current, animate = false)
    }
}

@Composable
private fun GridCanvas(
    puzzle: CrownsPuzzle,
    board: CrownsBoard,
    highlights: CrownsGridHighlights,
    palette: RegionPalette,
    winGlow: () -> Float = { 0f },
    animate: Boolean = rememberAnimationsEnabled()
) {
    val colors = MaterialTheme.colorScheme
    val crownTransitions = rememberCrownTransitions(puzzle, board.crowns, animate)
    val crownPainter = rememberVectorPainter(
        remember(palette) { crownVector(fill = palette.pieceFill, outline = palette.pieceOutline) }
    )
    val flaggedCrownPainter = rememberVectorPainter(
        remember(palette, colors.error) { crownVector(fill = colors.error, outline = palette.pieceOutline) }
    )
    val textMeasurer = rememberTextMeasurer()
    val questionStyle = TextStyle(color = palette.mark, fontWeight = FontWeight.Bold, fontSize = 20.sp)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val n = puzzle.size
        val geometry = GridGeometry(size.width, size.height, n, n)
        val cellSize = Size(geometry.cellWidth, geometry.cellHeight)
        fun topLeft(cell: Int) = Offset(geometry.cellLeft(cell), geometry.cellTop(cell))
        val corner = CornerRadius(cellSize.minDimension * TILE_CORNER)
        fun drawTile(cell: Int, color: Color) {
            val tile = tileRect(geometry, cell)
            drawRoundRect(color, tile.topLeft, tile.size, corner)
        }

        drawRoundRect(palette.board, cornerRadius = CornerRadius(cellSize.minDimension * BOARD_CORNER))
        for (cell in 0 until puzzle.cellCount) {
            drawTile(cell, palette.fillFor(puzzle.regionOf(cell)))
            when (cell) {
                in highlights.errorCells -> drawTile(cell, colors.error.copy(alpha = 0.45f))
                in highlights.hintCells -> drawTile(cell, colors.primary.copy(alpha = 0.45f))
                in highlights.hintAreaCells -> drawTile(cell, colors.primary.copy(alpha = 0.2f))
            }
        }

        for (cell in 0 until puzzle.cellCount) {
            val origin = topLeft(cell)
            val isFlagged = cell in highlights.conflictCells || cell in highlights.errorCells
            val transition = crownTransitions[cell]?.value
            fun drawCrown(progress: Float) {
                val scale = CrownsAnimations.CROWN_MIN_SCALE + (1f - CrownsAnimations.CROWN_MIN_SCALE) * progress
                val iconSize = cellSize.minDimension * CROWN_SIZE * scale
                val painter = if (isFlagged) flaggedCrownPainter else crownPainter
                translate(
                    left = origin.x + (cellSize.width - iconSize) / 2f,
                    top = origin.y + (cellSize.height - iconSize) / 2f
                ) {
                    with(painter) { draw(Size(iconSize, iconSize), alpha = progress) }
                }
            }
            when (board[cell]) {
                CellMark.EMPTY -> Unit
                CellMark.CROSS -> drawCross(origin, cellSize, palette.mark.copy(alpha = 0.8f))
                CellMark.QUESTION -> {
                    // 60% of the cell: still clearly readable on ~29dp cells (12×12 on a small phone).
                    val layout = textMeasurer.measure("?", questionStyle.copy(fontSize = (cellSize.height * 0.6f).toSp()))
                    drawText(
                        layout,
                        topLeft = origin + Offset(
                            (cellSize.width - layout.size.width) / 2f,
                            (cellSize.height - layout.size.height) / 2f
                        )
                    )
                }
                CellMark.CROWN -> drawCrown(transition ?: 1f)
            }
            // A crown just removed fades out over whatever the cell holds now.
            if (board[cell] != CellMark.CROWN && transition != null) drawCrown(transition)
            if (cell in highlights.conflictCells) {
                val tile = tileRect(geometry, cell)
                val stroke = 2.5.dp.toPx()
                drawRoundRect(
                    color = colors.error,
                    topLeft = tile.topLeft + Offset(stroke / 2, stroke / 2),
                    size = Size(tile.width - stroke, tile.height - stroke),
                    cornerRadius = corner,
                    style = Stroke(width = stroke)
                )
            }
        }

        val glow = winGlow()
        if (glow > 0f) {
            drawRect(palette.pieceFill.copy(alpha = WIN_GLOW_FILL_ALPHA * glow))
            drawRect(palette.pieceFill.copy(alpha = glow), style = Stroke(width = WIN_GLOW_BORDER.toPx()))
        }
    }
}

/** A crown fills this fraction of its cell. */
private const val CROWN_SIZE = 0.78f
private const val WIN_GLOW_FILL_ALPHA = 0.35f
private val WIN_GLOW_BORDER = 6.dp

private fun DrawScope.drawCross(origin: Offset, cell: Size, color: Color) {
    val inset = cell.minDimension * 0.3f
    val stroke = cell.minDimension * 0.07f
    drawLine(color, origin + Offset(inset, inset), origin + Offset(cell.width - inset, cell.height - inset), stroke)
    drawLine(color, origin + Offset(cell.width - inset, inset), origin + Offset(inset, cell.height - inset), stroke)
}

/** A cell's tile: a rounded square, with the same small gap all around it. */
private fun tileRect(geometry: GridGeometry, cell: Int): Rect {
    val inset = minOf(geometry.cellWidth, geometry.cellHeight) * TILE_GAP / 2f
    val left = geometry.cellLeft(cell)
    val top = geometry.cellTop(cell)
    return Rect(left + inset, top + inset, left + geometry.cellWidth - inset, top + geometry.cellHeight - inset)
}

/** Gap between tiles and tile and board corners, as fractions of a cell. */
private const val TILE_GAP = 0.07f
private const val TILE_CORNER = 0.16f
private const val BOARD_CORNER = 0.2f

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
