package com.payfacto.braingames.feature.wordsearch

import com.payfacto.braingames.feature.wordsearch.model.Direction
import com.payfacto.braingames.feature.wordsearch.model.WordEntry
import com.payfacto.braingames.feature.wordsearch.ui.wordListOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class WordListOrderTest {

    private fun entry(word: String, found: Boolean = false) = WordEntry(word, 0, 0, Direction.RIGHT, found)

    @Test
    fun `words to find come first, found words last, each group alphabetical`() {
        val words = listOf(entry("TIGRE", found = true), entry("PONT"), entry("ARBRE", found = true), entry("LUNE"))

        assertEquals(listOf("LUNE", "PONT", "ARBRE", "TIGRE"), wordListOrder(words).map { it.word })
    }
}
