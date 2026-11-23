package com.payfacto.braingames.feature.animals

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.animals.engine.AnimalsGenerator
import org.junit.Ignore
import org.junit.Test
import kotlin.random.Random

/**
 * Regenerates the Kotlin source of AnimalsFallbackPuzzles. Not part of the normal run:
 * remove @Ignore, run this test alone, and copy the printed block from the test report's
 * standard output into the bank. AnimalsFallbackPuzzlesTest then re-verifies every entry.
 */
class AnimalsFallbackPrinter {

    @Ignore("Run manually to regenerate the fallback bank")
    @Test
    fun printFallbackBank() {
        val generator = AnimalsGenerator(Random(FALLBACK_SEED))
        val code = buildString {
            Difficulty.entries.forEach { difficulty ->
                append("        Difficulty.${difficulty.name} to listOf(\n")
                repeat(PUZZLES_PER_DIFFICULTY) {
                    val puzzle = checkNotNull(generator.generate(difficulty))
                    val rows = puzzle.rowCells.joinToString(", ") { row ->
                        "\"" + row.joinToString("") { ('A' + puzzle.regionOf(it)).toString() } + "\""
                    }
                    append("            puzzle(\"${puzzle.solution.joinToString("")}\", $rows),\n")
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
