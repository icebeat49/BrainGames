package com.payfacto.braingames.feature.kakuro

import com.payfacto.braingames.feature.kakuro.engine.KakuroCombinations
import com.payfacto.braingames.feature.kakuro.engine.KakuroPuzzle
import com.payfacto.braingames.feature.kakuro.engine.KakuroSolver
import com.payfacto.braingames.feature.kakuro.engine.KakuroTechnique
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KakuroSolverTest {

    @Test
    fun `combinations list every digit set of a length and sum`() {
        assertEquals(listOf(0b11), KakuroCombinations.of(2, 3).toList()) // {1,2}
        assertEquals(listOf(0b111111111), KakuroCombinations.of(9, 45).toList())
        assertEquals(2, KakuroCombinations.of(2, 6).size) // {1,5} {2,4}
        assertEquals(0, KakuroCombinations.of(2, 18).size) // 9+9 repeats
    }

    @Test
    fun `a puzzle with one solution finds exactly that solution`() {
        val solutions = KakuroSolver.solutions(KakuroGrids.unique3.clues, limit = 2)

        assertEquals(1, solutions.size)
        assertArrayEquals(KakuroGrids.unique3.solution, solutions.single())
    }

    @Test
    fun `swappable digits give two solutions`() {
        assertEquals(2, KakuroSolver.countSolutions(KakuroGrids.ambiguous3.clues))
    }

    @Test
    fun `a puzzle settled by sum combinations alone is graded at that level`() {
        val grade = KakuroSolver.grade(KakuroGrids.unique3.clues)!!

        assertEquals(KakuroTechnique.SUM_COMBINATIONS, grade.hardest)
    }

    @Test
    fun `a puzzle with two solutions cannot be graded`() {
        assertNull(KakuroSolver.grade(KakuroGrids.ambiguous3.clues))
    }

    @Test
    fun `clues are the sums of the solution runs`() {
        val board = KakuroGrids.unique3.toBoard()

        assertEquals(
            com.payfacto.braingames.feature.kakuro.model.KakuroCell.Clue(downSum = 4, acrossSum = 0),
            board.cellAt(0, 1)
        )
        assertEquals(
            com.payfacto.braingames.feature.kakuro.model.KakuroCell.Clue(downSum = 0, acrossSum = 3),
            board.cellAt(1, 0)
        )
    }
}

/** Hand-made grids; 0 = block. */
object KakuroGrids {
    /** 1 2 / 3 1: across 3 and 4, down 4 and 3. Only one fill works (2 2 would repeat). */
    val unique3 = KakuroPuzzle(3, intArrayOf(0, 0, 0, 0, 1, 2, 0, 3, 1))

    /** 1 2 / 2 1: every clue is 3, and 2 1 / 1 2 fits too. */
    val ambiguous3 = KakuroPuzzle(3, intArrayOf(0, 0, 0, 0, 1, 2, 0, 2, 1))
}
