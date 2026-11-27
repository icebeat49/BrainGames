package com.payfacto.braingames.feature.kakuro

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.kakuro.KakuroGeneratorTest.Companion.assertPuzzleIsGood
import com.payfacto.braingames.feature.kakuro.data.KakuroBankFormat
import com.payfacto.braingames.feature.kakuro.engine.KakuroDifficulty
import org.junit.Assert.assertEquals
import org.junit.Test

/** Every puzzle shipped in res/raw is checked like a freshly generated one. */
class KakuroBankTest {

    @Test
    fun `each size ships 250 distinct puzzles`() {
        Difficulty.entries.forEach { difficulty ->
            val puzzles = KakuroBankFiles.load(KakuroDifficulty.sizeFor(difficulty))
            assertEquals("$difficulty count", BANK_SIZE, puzzles.size)
            assertEquals("$difficulty distinct", BANK_SIZE, puzzles.toSet().size)
        }
    }

    @Test
    fun `every bundled puzzle is valid, unique, solvable by logic and at its difficulty`() {
        Difficulty.entries.forEach { difficulty ->
            val size = KakuroDifficulty.sizeFor(difficulty)
            KakuroBankFiles.load(size).forEachIndexed { index, puzzle ->
                assertPuzzleIsGood(puzzle, difficulty, "bank ${size}x$size #$index")
            }
        }
    }

    @Test
    fun `a bank line reads back to the same puzzle`() {
        val puzzle = KakuroBankFiles.load(KakuroDifficulty.sizeFor(Difficulty.EASY)).first()

        assertEquals(puzzle, KakuroBankFormat.decode(KakuroBankFormat.encode(puzzle)))
    }

    private companion object {
        const val BANK_SIZE = 250
    }
}
