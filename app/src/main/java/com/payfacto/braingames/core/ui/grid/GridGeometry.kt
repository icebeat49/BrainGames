package com.payfacto.braingames.core.ui.grid

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

/** A position in grid units: row 2.5 is the middle of the third row. */
data class GridPoint(val row: Float, val column: Float)

/**
 * Pixel ↔ cell maths for a grid drawn edge to edge in a [width] × [height] area.
 * Cells are numbered row-major. Pure Kotlin, so gesture logic using it stays testable.
 */
class GridGeometry(
    val width: Float,
    val height: Float,
    val rows: Int,
    val columns: Int
) {
    val cellWidth: Float get() = width / columns
    val cellHeight: Float get() = height / rows

    fun toGridPoint(x: Float, y: Float) = GridPoint(row = y / cellHeight, column = x / cellWidth)

    /** The cell under a pixel, or null outside the grid. */
    fun cellAt(x: Float, y: Float): Int? = cellAt(toGridPoint(x, y), rows, columns)

    fun cellLeft(cell: Int): Float = (cell % columns) * cellWidth
    fun cellTop(cell: Int): Float = (cell / columns) * cellHeight

    companion object {

        fun cellAt(point: GridPoint, rows: Int, columns: Int): Int? {
            val row = floor(point.row).toInt()
            val col = floor(point.column).toInt()
            return if (row in 0 until rows && col in 0 until columns) row * columns + col else null
        }

        /**
         * Every cell the straight segment [from] → [to] passes through, in order, skipping
         * those outside the grid. Exact grid traversal (Amanatides–Woo), not sampling, so a
         * fast swipe between two pointer samples cannot skip a cell. A segment through a
         * corner takes one of the two side cells.
         */
        fun cellsAlong(from: GridPoint, to: GridPoint, rows: Int, columns: Int): List<Int> {
            var row = floor(from.row).toInt()
            var col = floor(from.column).toInt()
            val endRow = floor(to.row).toInt()
            val endCol = floor(to.column).toInt()
            val dRow = to.row - from.row
            val dCol = to.column - from.column
            val stepRow = sign(dRow).toInt()
            val stepCol = sign(dCol).toInt()

            // Distance along the segment (0..1) to the next row / column boundary, and per cell.
            var tMaxRow = boundaryT(from.row, row, stepRow, dRow)
            var tMaxCol = boundaryT(from.column, col, stepCol, dCol)
            val tDeltaRow = if (stepRow != 0) abs(1f / dRow) else Float.POSITIVE_INFINITY
            val tDeltaCol = if (stepCol != 0) abs(1f / dCol) else Float.POSITIVE_INFINITY

            val cells = mutableListOf<Int>()
            fun visit() {
                if (row in 0 until rows && col in 0 until columns) cells += row * columns + col
            }

            visit()
            var remaining = abs(endRow - row) + abs(endCol - col)
            while (remaining > 0) {
                if (tMaxCol <= tMaxRow) {
                    col += stepCol
                    tMaxCol += tDeltaCol
                } else {
                    row += stepRow
                    tMaxRow += tDeltaRow
                }
                visit()
                remaining--
            }
            return cells
        }

        private fun boundaryT(start: Float, cell: Int, step: Int, delta: Float): Float = when {
            step > 0 -> (cell + 1 - start) / delta
            step < 0 -> (cell - start) / delta
            else -> Float.POSITIVE_INFINITY
        }
    }
}
