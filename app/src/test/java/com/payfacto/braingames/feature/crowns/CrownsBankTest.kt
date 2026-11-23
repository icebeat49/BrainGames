package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.CrownsGeneratorTest.Companion.assertPuzzleIsGood
import com.payfacto.braingames.feature.crowns.engine.CrownsDifficulty
import org.junit.Assert.assertEquals
import org.junit.Test

/** Every puzzle shipped in res/raw is checked like a freshly generated one. */
class CrownsBankTest {

    @Test
    fun `each size ships 250 distinct puzzles`() {
        Difficulty.entries.forEach { difficulty ->
            val puzzles = CrownsBankFiles.load(CrownsDifficulty.sizeFor(difficulty))
            assertEquals("$difficulty count", BANK_SIZE, puzzles.size)
            assertEquals("$difficulty distinct", BANK_SIZE, puzzles.toSet().size)
        }
    }

    @Test
    fun `every bundled puzzle is valid, unique, logically solvable and at its difficulty`() {
        Difficulty.entries.forEach { difficulty ->
            val size = CrownsDifficulty.sizeFor(difficulty)
            CrownsBankFiles.load(size).forEachIndexed { index, puzzle ->
                assertPuzzleIsGood(puzzle, difficulty, "bank ${size}x$size #$index")
            }
        }
    }

    private companion object {
        const val BANK_SIZE = 250
    }
}
