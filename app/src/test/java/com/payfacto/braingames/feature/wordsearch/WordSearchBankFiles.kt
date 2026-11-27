package com.payfacto.braingames.feature.wordsearch

import com.payfacto.braingames.feature.wordsearch.data.WordSearchBankFormat
import com.payfacto.braingames.feature.wordsearch.data.WordSearchBankRules
import com.payfacto.braingames.feature.wordsearch.model.BankPuzzle
import java.io.File

/** The bundled bank files and the word lists they are built from (unit tests run in the app module). */
object WordSearchBankFiles {
    val LANGUAGES = listOf("en", "fr")
    val SIZES = listOf(12, 18)

    fun file(language: String, size: Int) = File("src/main/res/raw/wordsearch_bank_${language}_$size.txt")

    fun load(language: String, size: Int): List<BankPuzzle> = WordSearchBankFormat.parse(file(language, size).readText())

    fun words(language: String): List<String> = wordList("words_$language.txt")

    fun secrets(language: String): List<String> = wordList("secrets_$language.txt")

    /** Secret lengths left over: about 5% of a 12x12 grid, 3-5% of an 18x18 one. */
    fun secretLengthsFor(size: Int): IntRange = if (size <= 12) 6..10 else 10..16

    fun rules(language: String, size: Int) = WordSearchBankRules(size, words(language))

    private fun wordList(name: String): List<String> =
        File("src/test/resources/wordsearch/$name").readLines()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
}
