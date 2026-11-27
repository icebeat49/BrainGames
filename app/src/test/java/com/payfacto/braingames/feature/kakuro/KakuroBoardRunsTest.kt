package com.payfacto.braingames.feature.kakuro

import com.payfacto.braingames.testutil.TestBoards
import org.junit.Assert.assertEquals
import org.junit.Test

/** The row and column highlighted around the selected Kakuro cell. */
class KakuroBoardRunsTest {

    private val board = TestBoards.kakuro()

    @Test
    fun `a cell's runs are its row and its column, up to the clues`() {
        assertEquals(setOf(1 to 1, 1 to 2, 2 to 1), board.runCellsThrough(1, 1))
    }

    @Test
    fun `a clue or block cell has no runs`() {
        assertEquals(emptySet<Pair<Int, Int>>(), board.runCellsThrough(0, 1))
        assertEquals(emptySet<Pair<Int, Int>>(), board.runCellsThrough(0, 0))
    }
}
