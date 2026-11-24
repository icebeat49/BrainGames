package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.engine.CrownsDifficulty
import com.payfacto.braingames.feature.crowns.engine.CrownsGenerationRules
import com.payfacto.braingames.feature.crowns.engine.CrownsGenerator
import com.payfacto.braingames.feature.crowns.engine.CrownsSolver
import com.payfacto.braingames.feature.crowns.engine.CrownsValidator
import com.payfacto.braingames.feature.crowns.engine.PuzzleProblem
import com.payfacto.braingames.feature.crowns.engine.Technique
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class CrownsGeneratorTest {

    @Test
    fun `generated puzzles follow every rule, with single-cell regions at the target frequency`() {
        warmUp()
        val report = StringBuilder(
            "\nCrowns generation, $PUZZLES_PER_DIFFICULTY puzzles per difficulty (JVM, after warm-up)\n"
        )
        val failuresByDifficulty = mutableMapOf<Difficulty, Int>()
        val frequencyMisses = mutableListOf<String>()

        for (difficulty in Difficulty.entries) {
            val stats = sample(difficulty)
            failuresByDifficulty[difficulty] = stats.failures
            report.append(stats.reportLine())
            val target = CrownsGenerationRules.forDifficulty(difficulty).singleCellChance
            if (abs(stats.singleCellFrequency - target) > FREQUENCY_TOLERANCE) {
                frequencyMisses += "$difficulty: %.2f vs target %.2f".format(stats.singleCellFrequency, target)
            }
        }
        println(report)
        assertEquals("generator gave up: $failuresByDifficulty", 0, failuresByDifficulty.values.sum())
        assertEquals("single-cell frequency off target", emptyList<String>(), frequencyMisses)
    }

    private fun sample(difficulty: Difficulty): SampleStats {
        val generator = CrownsGenerator(Random(SEED + difficulty.ordinal))
        val stats = SampleStats(difficulty)
        repeat(PUZZLES_PER_DIFFICULTY) { index ->
            val start = System.nanoTime()
            val puzzle = generator.generate(difficulty)
            stats.timesMs += (System.nanoTime() - start) / 1_000_000.0
            if (puzzle == null) {
                stats.failures++
            } else {
                val technique = assertPuzzleIsGood(puzzle, difficulty, "$difficulty #$index")
                stats.add(puzzle, technique)
            }
        }
        return stats
    }

    private class SampleStats(val difficulty: Difficulty) {
        val timesMs = mutableListOf<Double>()
        var failures = 0
        private var puzzles = 0
        private var withSingleCell = 0
        private var smallestRegionTotal = 0
        private var immediateCrownsTotal = 0
        private val hardest = mutableMapOf<Technique, Int>()

        val singleCellFrequency: Double get() = withSingleCell.toDouble() / puzzles

        fun add(puzzle: CrownsPuzzle, technique: Technique) {
            val sizes = puzzle.regionCells.map { it.size }
            puzzles++
            if (1 in sizes) withSingleCell++
            smallestRegionTotal += sizes.min()
            immediateCrownsTotal += CrownsSolver.immediateCrowns(puzzle)
            hardest.merge(technique, 1, Int::plus)
        }

        fun reportLine(): String {
            val sorted = timesMs.sorted()
            fun percentile(p: Double) = sorted[((sorted.size - 1) * p).toInt()]
            val mix = Technique.entries.joinToString(" ") { "${it.name.take(6)}=${hardest[it] ?: 0}" }
            return ("%-9s N=%2d  1-cell %3.0f%%  smallest %.2f  immediate %.2f  " +
                "median %6.1f ms  p95 %7.1f  max %7.1f  failures %d   hardest: %s\n").format(
                difficulty, CrownsDifficulty.sizeFor(difficulty), singleCellFrequency * 100,
                smallestRegionTotal.toDouble() / puzzles, immediateCrownsTotal.toDouble() / puzzles,
                percentile(0.5), percentile(0.95), sorted.last(), failures, mix
            )
        }
    }

    @Test
    fun `a generator with no budget gives up instead of returning a bad puzzle`() {
        assertEquals(null, CrownsGenerator(Random(1), maxLayouts = 0).generate(Difficulty.EXPERT))
    }

    @Test
    fun `the same seed gives the same puzzle`() {
        val first = CrownsGenerator(Random(99)).generate(Difficulty.MEDIUM)
        val second = CrownsGenerator(Random(99)).generate(Difficulty.MEDIUM)

        assertTrue(first != null && first == second)
    }

    private fun warmUp() {
        val generator = CrownsGenerator(Random(1))
        Difficulty.entries.forEach { difficulty -> repeat(WARM_UP_ROUNDS) { generator.generate(difficulty) } }
    }

    companion object {
        const val PUZZLES_PER_DIFFICULTY = 200
        private const val FREQUENCY_TOLERANCE = 0.10
        private const val WARM_UP_ROUNDS = 5
        private const val SEED = 20261123L

        /** The checks every puzzle must pass, generated or from the bank. Returns the hardest technique. */
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
            assertRegionRules(puzzle, difficulty, label)
            return result.hardest ?: Technique.SINGLE_CANDIDATE
        }

        /** Region sizes and immediate crowns within [CrownsGenerationRules] for [difficulty]. */
        fun assertRegionRules(puzzle: CrownsPuzzle, difficulty: Difficulty, label: String) {
            val rules = CrownsGenerationRules.forDifficulty(difficulty)
            val sizes = puzzle.regionCells.map { it.size }
            assertTrue("$label single-cell regions: $sizes", sizes.count { it == 1 } <= 1)
            assertTrue("$label region sizes $sizes for $difficulty", CrownsGenerationRules.shapeIsAllowed(sizes, rules))
            val immediate = CrownsSolver.immediateCrowns(puzzle)
            assertTrue(
                "$label immediate crowns $immediate > ${rules.maxImmediateCrowns}",
                immediate <= rules.maxImmediateCrowns
            )
        }
    }
}
