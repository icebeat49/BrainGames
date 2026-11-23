package com.payfacto.braingames.feature.crowns.data

import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle

/**
 * Text format of the bundled puzzle bank (res/raw/crowns_bank_<size>.txt): one puzzle per
 * line, `solution|regions`, where the solution is one letter per row giving the crown's
 * column ('A' = column 0) and the regions are size×size letters, row by row ('A' = region 0).
 * Lines starting with '#' and blank lines are ignored.
 *
 * Plain text rather than JSON so the same parser runs in JVM unit tests (org.json is only a
 * stub there), which re-verify every shipped puzzle.
 */
object CrownsBankFormat {
    private const val SEPARATOR = '|'
    private const val COMMENT = '#'

    fun encode(puzzle: CrownsPuzzle): String =
        puzzle.solution.joinToString("") { letter(it) } + SEPARATOR +
            puzzle.regions.joinToString("") { letter(it) }

    fun decode(line: String): CrownsPuzzle {
        val (solution, regions) = line.trim().split(SEPARATOR)
        return CrownsPuzzle(
            size = solution.length,
            regions = regions.map { it - 'A' }.toIntArray(),
            solution = solution.map { it - 'A' }.toIntArray()
        )
    }

    fun parse(text: String): List<CrownsPuzzle> =
        text.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith(COMMENT) }
            .map(::decode)
            .toList()

    fun write(puzzles: Collection<CrownsPuzzle>, header: String): String = buildString {
        header.lines().forEach { append(COMMENT).append(' ').append(it).append('\n') }
        puzzles.forEach { append(encode(it)).append('\n') }
    }

    private fun letter(value: Int) = ('A' + value).toString()
}
