package com.payfacto.braingames.feature.animals

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.animals.AnimalsGeneratorTest.Companion.assertPuzzleIsGood
import com.payfacto.braingames.feature.animals.data.AnimalsFallbackPuzzles
import com.payfacto.braingames.feature.animals.data.AnimalsPuzzles
import com.payfacto.braingames.feature.animals.engine.AnimalsGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AnimalsFallbackPuzzlesTest {

    @Test
    fun `every fallback puzzle passes the same checks as generated ones`() {
        Difficulty.entries.forEach { difficulty ->
            AnimalsFallbackPuzzles.all(difficulty).forEachIndexed { index, puzzle ->
                assertPuzzleIsGood(puzzle, difficulty, "fallback $difficulty #$index")
            }
        }
    }

    @Test
    fun `there are three distinct fallback puzzles per difficulty`() {
        Difficulty.entries.forEach { difficulty ->
            assertEquals(3, AnimalsFallbackPuzzles.all(difficulty).toSet().size)
        }
    }

    @Test
    fun `a generator with no budget gives up instead of returning a bad puzzle`() {
        val generator = AnimalsGenerator(Random(1), maxLayouts = 0)

        assertEquals(null, generator.generate(Difficulty.EXPERT))
    }

    @Test
    fun `newPuzzle returns a good puzzle for every difficulty`() {
        Difficulty.entries.forEach { difficulty ->
            assertPuzzleIsGood(AnimalsPuzzles.newPuzzle(difficulty, Random(42)), difficulty, "newPuzzle $difficulty")
        }
    }

    @Test
    fun `the same seed gives the same puzzle`() {
        val first = AnimalsGenerator(Random(99)).generate(Difficulty.HARD)
        val second = AnimalsGenerator(Random(99)).generate(Difficulty.HARD)

        assertTrue(first != null && first == second)
    }
}
