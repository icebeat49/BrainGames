package com.payfacto.braingames.feature.kakuro

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.kakuro.engine.KakuroDifficulty
import com.payfacto.braingames.feature.kakuro.engine.KakuroGenerator
import com.payfacto.braingames.feature.kakuro.engine.KakuroPuzzle
import com.payfacto.braingames.feature.kakuro.engine.KakuroSolver
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class KakuroGeneratorTest {

    @Test
    fun `generated puzzles are good at every difficulty`() {
        Difficulty.STANDARD.forEach { difficulty ->
            val generator = KakuroGenerator(Random(difficulty.ordinal.toLong()))
            val rules = KakuroDifficulty.rulesFor(difficulty)
            var checked = 0
            while (checked < PUZZLES_PER_DIFFICULTY) {
                val puzzle = generator.generate(rules) ?: continue
                val grade = KakuroSolver.grade(puzzle.clues) ?: continue
                if (!KakuroDifficulty.accepts(difficulty, grade)) continue
                assertPuzzleIsGood(puzzle, difficulty, "$difficulty #$checked")
                checked++
            }
        }
    }

    @Test
    fun `the same seed gives the same puzzle`() {
        assertEquals(firstPuzzle(seed = 7), firstPuzzle(seed = 7))
    }

    private fun firstPuzzle(seed: Long): KakuroPuzzle {
        val generator = KakuroGenerator(Random(seed))
        val rules = KakuroDifficulty.rulesFor(Difficulty.MEDIUM)
        return (1..MAX_ATTEMPTS).firstNotNullOf { generator.generate(rules) }
    }

    companion object {
        private const val PUZZLES_PER_DIFFICULTY = 3
        private const val MAX_ATTEMPTS = 1_000

        /** What every shipped puzzle must satisfy. */
        fun assertPuzzleIsGood(puzzle: KakuroPuzzle, difficulty: Difficulty, label: String) {
            val rules = KakuroDifficulty.rulesFor(difficulty)
            assertEquals("$label size", rules.size, puzzle.size)

            puzzle.runs.forEach { run ->
                assertTrue("$label run length ${run.length}", run.length in 2..rules.maxRunLength)
                val digits = run.cells.map(puzzle::digitAt)
                assertEquals("$label repeated digit in a run", digits.size, digits.toSet().size)
                assertEquals("$label clue", digits.sum(), run.sum)
            }
            puzzle.whiteCells.forEach { cell ->
                val n = puzzle.size
                val mirror = (n - cell / n) * n + (n - cell % n)
                assertTrue("$label not symmetric at $cell", puzzle.isWhite(mirror))
            }

            val solutions = KakuroSolver.solutions(puzzle.clues, limit = 2)
            assertEquals("$label solution count", 1, solutions.size)
            assertArrayEquals("$label solution", puzzle.solution, solutions.single())

            val grade = KakuroSolver.grade(puzzle.clues)
            assertNotNull("$label not solvable by logic", grade)
            assertTrue("$label graded $grade", KakuroDifficulty.accepts(difficulty, grade!!))
        }
    }
}
