package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.CrownsGeneratorTest.Companion.assertPuzzleIsGood
import com.payfacto.braingames.feature.crowns.engine.CrownsDifficulty
import com.payfacto.braingames.feature.crowns.engine.CrownsGenerationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

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
    fun `every bundled puzzle is valid, unique, logically solvable, at its difficulty and within its region rules`() {
        Difficulty.entries.forEach { difficulty ->
            val size = CrownsDifficulty.sizeFor(difficulty)
            CrownsBankFiles.load(size).forEachIndexed { index, puzzle ->
                assertPuzzleIsGood(puzzle, difficulty, "bank ${size}x$size #$index")
            }
        }
    }

    @Test
    fun `the bank has single-cell regions at the target frequency`() {
        Difficulty.entries.forEach { difficulty ->
            val puzzles = CrownsBankFiles.load(CrownsDifficulty.sizeFor(difficulty))
            val withSingleCell = puzzles.count { puzzle -> puzzle.regionCells.any { it.size == 1 } }
            val frequency = withSingleCell.toDouble() / puzzles.size
            val target = CrownsGenerationRules.forDifficulty(difficulty).singleCellChance
            assertTrue(
                "$difficulty: %.2f vs target %.2f".format(frequency, target),
                abs(frequency - target) <= FREQUENCY_TOLERANCE
            )
        }
    }

    private companion object {
        const val BANK_SIZE = 250
        const val FREQUENCY_TOLERANCE = 0.10
    }
}
