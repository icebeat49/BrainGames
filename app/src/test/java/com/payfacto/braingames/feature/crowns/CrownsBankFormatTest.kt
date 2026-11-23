package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.feature.crowns.CrownsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.crowns.data.CrownsBankFormat
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import org.junit.Assert.assertEquals
import org.junit.Test

class CrownsBankFormatTest {

    @Test
    fun `a puzzle survives encode and decode`() {
        val line = CrownsBankFormat.encode(uniqueFiveByFive)

        assertEquals("ACEBD|ABBCCBBBCCDDBCCDDEECDEEEE", line)
        assertEquals(uniqueFiveByFive, CrownsBankFormat.decode(line))
    }

    @Test
    fun `columns beyond 9 are letters, so 11x11 fits`() {
        val solution = intArrayOf(10, 8, 6, 4, 2, 0, 9, 7, 5, 3, 1)
        val puzzle = CrownsPuzzle(11, IntArray(121) { it / 11 }, solution)

        assertEquals(puzzle, CrownsBankFormat.decode(CrownsBankFormat.encode(puzzle)))
    }

    @Test
    fun `comments and blank lines are ignored`() {
        val text = CrownsBankFormat.write(listOf(uniqueFiveByFive, uniqueFiveByFive), header = "a header\nsecond line") +
            "\n   \n"

        assertEquals(listOf(uniqueFiveByFive, uniqueFiveByFive), CrownsBankFormat.parse(text))
    }
}
