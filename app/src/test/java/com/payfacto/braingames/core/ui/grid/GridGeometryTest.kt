package com.payfacto.braingames.core.ui.grid

import com.payfacto.braingames.core.ui.grid.GridGeometry.Companion.cellsAlong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GridGeometryTest {

    private val geometry = GridGeometry(width = 700f, height = 700f, rows = 7, columns = 7)

    @Test
    fun `a pixel maps to its cell`() {
        assertEquals(0, geometry.cellAt(10f, 10f))
        assertEquals(7 * 3 + 2, geometry.cellAt(250f, 350f))
        assertEquals(48, geometry.cellAt(699f, 699f))
    }

    @Test
    fun `a pixel outside the grid maps to no cell`() {
        assertNull(geometry.cellAt(-1f, 10f))
        assertNull(geometry.cellAt(10f, 700f))
    }

    @Test
    fun `cell origins are in pixels`() {
        assertEquals(200f, geometry.cellLeft(7 * 3 + 2))
        assertEquals(300f, geometry.cellTop(7 * 3 + 2))
    }

    @Test
    fun `a fast horizontal swipe visits every cell in between`() {
        val cells = cellsAlong(GridPoint(2.5f, 0.2f), GridPoint(2.5f, 6.8f), 7, 7)

        assertEquals((0..6).map { 2 * 7 + it }, cells)
    }

    @Test
    fun `a swipe backwards visits cells in reverse order`() {
        val cells = cellsAlong(GridPoint(0.5f, 3.5f), GridPoint(0.5f, 0.5f), 7, 7)

        assertEquals(listOf(3, 2, 1, 0), cells)
    }

    @Test
    fun `a shallow diagonal visits each cell it crosses and no gaps`() {
        // From the middle of (0,0) to the middle of (2,6): every step moves to an edge neighbour.
        val cells = cellsAlong(GridPoint(0.5f, 0.5f), GridPoint(2.5f, 6.5f), 7, 7)

        assertEquals(0, cells.first())
        assertEquals(2 * 7 + 6, cells.last())
        cells.zipWithNext().forEach { (a, b) ->
            val step = kotlin.math.abs(a / 7 - b / 7) + kotlin.math.abs(a % 7 - b % 7)
            assertEquals("cells $a → $b must share an edge", 1, step)
        }
    }

    @Test
    fun `a segment inside one cell visits only that cell`() {
        assertEquals(listOf(8), cellsAlong(GridPoint(1.1f, 1.1f), GridPoint(1.9f, 1.8f), 7, 7))
    }

    @Test
    fun `parts of a swipe outside the grid are ignored`() {
        val cells = cellsAlong(GridPoint(0.5f, -2.5f), GridPoint(0.5f, 1.5f), 7, 7)

        assertEquals(listOf(0, 1), cells)
    }
}
