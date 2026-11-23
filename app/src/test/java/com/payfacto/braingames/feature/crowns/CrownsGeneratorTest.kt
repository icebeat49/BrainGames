package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.engine.CrownsDifficulty
import com.payfacto.braingames.feature.crowns.engine.CrownsGenerator
import com.payfacto.braingames.feature.crowns.engine.CrownsSolver
import com.payfacto.braingames.feature.crowns.engine.CrownsValidator
import com.payfacto.braingames.feature.crowns.engine.PuzzleProblem
import com.payfacto.braingames.feature.crowns.engine.Technique
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CrownsGeneratorTest {

    @Test
    fun `every generated puzzle is valid, unique, logically solvable and at its difficulty`() {
        warmUp()
        val report = StringBuilder("\nCrowns generation, $PUZZLES_PER_DIFFICULTY puzzles per difficulty (JVM, after warm-up)\n")

        for (difficulty in Difficulty.entries) {
            val generator = CrownsGenerator(Random(SEED + difficulty.ordinal))
            val timesMs = mutableListOf<Double>()
            val hardest = mutableMapOf<Technique, Int>()

            repeat(PUZZLES_PER_DIFFICULTY) { index ->
                val start = System.nanoTime()
                val puzzle = generator.generate(difficulty)
                timesMs += (System.nanoTime() - start) / 1_000_000.0

                assertNotNull("$difficulty #$index: generator gave up", puzzle)
                val technique = assertPuzzleIsGood(puzzle!!, difficulty, "$difficulty #$index")
                hardest.merge(technique, 1, Int::plus)
            }
            report.append(timingLine(difficulty, timesMs, hardest))
        }
        println(report)
    }

    private fun warmUp() {
        val generator = CrownsGenerator(Random(1))
        Difficulty.entries.forEach { difficulty -> repeat(WARM_UP_ROUNDS) { generator.generate(difficulty) } }
    }

    private fun timingLine(difficulty: Difficulty, timesMs: List<Double>, hardest: Map<Technique, Int>): String {
        val sorted = timesMs.sorted()
        fun percentile(p: Double) = sorted[((sorted.size - 1) * p).toInt()]
        val mix = Technique.entries.joinToString(" ") { "${it.name.take(6)}=${hardest[it] ?: 0}" }
        return "%-9s N=%d  mean %6.1f ms  median %6.1f  p95 %6.1f  max %6.1f   hardest: %s\n".format(
            difficulty, CrownsDifficulty.sizeFor(difficulty), timesMs.average(),
            percentile(0.5), percentile(0.95), sorted.last(), mix
        )
    }

    companion object {
        const val PUZZLES_PER_DIFFICULTY = 50
        private const val WARM_UP_ROUNDS = 5
        private const val SEED = 20261123L

        /** The checks every puzzle must pass, generated or from the fallback bank. Returns the hardest technique. */
        fun assertPuzzleIsGood(puzzle: CrownsPuzzle, difficulty: Difficulty, label: String): Technique {
            assertEquals("$label size", CrownsDifficulty.sizeFor(difficulty), puzzle.size)
            assertEquals("$label shape", emptySet<PuzzleProblem>(), CrownsValidator.puzzleProblems(puzzle))
            assertEquals("$label unique", 1, CrownsSolver.countSolutions(puzzle, limit = 2))
            val result = CrownsSolver.solve(puzzle)
            assertTrue("$label logically solvable", result.solved)
            assertTrue(
                "$label difficulty: hardest ${result.hardest}",
                CrownsDifficulty.accepts(difficulty, result.hardest)
            )
            return result.hardest ?: Technique.SINGLE_CANDIDATE
        }
    }
}
