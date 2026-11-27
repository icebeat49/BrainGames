package com.payfacto.braingames.feature.kakuro.data

import com.payfacto.braingames.feature.kakuro.engine.KakuroPuzzle
import kotlin.math.sqrt

/**
 * Text format of the bundled puzzle bank (res/raw/kakuro_bank_<size>.txt): one puzzle per
 * line, the solution grid row by row, `.` for a block and the digit for a white cell. The
 * clues are not stored: they are the sums of the solution runs. Lines starting with '#' and
 * blank lines are ignored.
 *
 * Plain text rather than JSON so the same parser runs in JVM unit tests (org.json is only a
 * stub there), which re-verify every shipped puzzle.
 */
object KakuroBankFormat {
    private const val BLOCK = '.'
    private const val COMMENT = '#'

    fun encode(puzzle: KakuroPuzzle): String =
        puzzle.solution.joinToString("") { if (it == KakuroPuzzle.BLOCK) BLOCK.toString() else it.toString() }

    fun decode(line: String): KakuroPuzzle {
        val text = line.trim()
        val size = sqrt(text.length.toDouble()).toInt()
        require(size * size == text.length) { "a puzzle line must be square, got ${text.length} cells" }
        return KakuroPuzzle(size, IntArray(text.length) { if (text[it] == BLOCK) KakuroPuzzle.BLOCK else text[it] - '0' })
    }

    fun parse(text: String): List<KakuroPuzzle> =
        text.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith(COMMENT) }
            .map(::decode)
            .toList()

    fun write(puzzles: Collection<KakuroPuzzle>, header: String): String = buildString {
        header.lines().forEach { append(COMMENT).append(' ').append(it).append('\n') }
        puzzles.forEach { append(encode(it)).append('\n') }
    }
}
