package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.CrownsGeneratorTest.Companion.assertPuzzleIsGood
import com.payfacto.braingames.feature.crowns.data.CrownsFallbackPuzzles
import com.payfacto.braingames.feature.crowns.data.CrownsPuzzles
import com.payfacto.braingames.feature.crowns.engine.CrownsGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CrownsFallbackPuzzlesTest {

    @Test
    fun `every fallback puzzle passes the same checks as generated ones`() {
        Difficulty.entries.forEach { difficulty ->
            CrownsFallbackPuzzles.all(difficulty).forEachIndexed { index, puzzle ->
                assertPuzzleIsGood(puzzle, difficulty, "fallback $difficulty #$index")
            }
        }
    }

    @Test
    fun `there are three distinct fallback puzzles per difficulty`() {
        Difficulty.entries.forEach { difficulty ->
            assertEquals(3, CrownsFallbackPuzzles.all(difficulty).toSet().size)
        }
    }

    @Test
    fun `a generator with no budget gives up instead of returning a bad puzzle`() {
        val generator = CrownsGenerator(Random(1), maxLayouts = 0)

        assertEquals(null, generator.generate(Difficulty.EXPERT))
    }

    @Test
    fun `newPuzzle returns a good puzzle for every difficulty`() {
        Difficulty.entries.forEach { difficulty ->
            assertPuzzleIsGood(CrownsPuzzles.newPuzzle(difficulty, Random(42)), difficulty, "newPuzzle $difficulty")
        }
    }

    @Test
    fun `the same seed gives the same puzzle`() {
        val first = CrownsGenerator(Random(99)).generate(Difficulty.HARD)
        val second = CrownsGenerator(Random(99)).generate(Difficulty.HARD)

        assertTrue(first != null && first == second)
    }
}
