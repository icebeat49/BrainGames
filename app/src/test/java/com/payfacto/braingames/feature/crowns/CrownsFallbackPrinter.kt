package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.engine.CrownsGenerator
import org.junit.Ignore
import org.junit.Test
import kotlin.random.Random

/**
 * Regenerates the Kotlin source of CrownsFallbackPuzzles. Not part of the normal run:
 * remove @Ignore, run this test alone, and copy the printed block from the test report's
 * standard output into the bank. CrownsFallbackPuzzlesTest then re-verifies every entry.
 */
class CrownsFallbackPrinter {

    @Ignore("Run manually to regenerate the fallback bank")
    @Test
    fun printFallbackBank() {
        val generator = CrownsGenerator(Random(FALLBACK_SEED))
        val code = buildString {
            Difficulty.entries.forEach { difficulty ->
                append("        Difficulty.${difficulty.name} to listOf(\n")
                repeat(PUZZLES_PER_DIFFICULTY) {
                    val puzzle = checkNotNull(generator.generate(difficulty))
                    val rows = puzzle.rowCells.joinToString(", ") { row ->
                        "\"" + row.joinToString("") { ('A' + puzzle.regionOf(it)).toString() } + "\""
                    }
                    val solution = puzzle.solution.joinToString("") { ('A' + it).toString() }
                    append("            puzzle(\"$solution\", $rows),\n")
                }
                append("        ),\n")
            }
        }
        println(code)
    }

    private companion object {
        const val FALLBACK_SEED = 7_000_000L
        const val PUZZLES_PER_DIFFICULTY = 3
    }
}
