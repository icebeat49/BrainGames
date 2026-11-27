package com.payfacto.braingames.feature.wordsearch

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.wordsearch.data.FallbackPuzzles
import com.payfacto.braingames.feature.wordsearch.data.WordSearchGenerator
import com.payfacto.braingames.feature.wordsearch.model.Puzzle
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchCorrectnessTest {

    private val enWords = listOf(
        "CAT", "DOG", "SUN", "MAP", "HAT", "BEE", "CUP", "PEN",
        "MOON", "STAR", "BIRD", "FISH", "TREE", "BOOK", "RAIN", "FIRE", "WIND", "SNOW",
        "FROG", "BEAR", "LION", "WOLF", "DUCK", "CROW",
        "APPLE", "GRAPE", "LEMON", "MANGO", "PEACH", "PLUMB", "PIANO", "FLUTE", "OCEAN",
        "RIVER", "CLOUD", "EARTH", "FLAME", "GLASS", "HOUSE", "LIGHT", "MUSIC", "NIGHT",
        "PAINT", "QUEEN", "ROBOT", "SHOES", "TRAIN", "WATER", "YOUTH", "ZEBRA",
        "CASTLE", "DRAGON", "FOREST", "GARDEN", "BRIDGE", "FLOWER", "PLANET", "ROCKET",
        "SILVER", "SUMMER", "WINTER", "AUTUMN", "SPRING", "YELLOW", "PURPLE", "ORANGE",
        "BUTTER", "CANDLE", "DANCER", "FALCON", "HARBOR", "ISLAND", "JUNGLE", "KITTEN",
        "LAGOON", "MARBLE", "NATURE", "PEBBLE", "SHADOW", "TURTLE", "VIOLET", "WIZARD",
        "COMPASS", "DIAMOND", "ECLIPSE", "FANTASY", "GLITTER", "HARVEST", "JOURNEY",
        "LANTERN", "MYSTERY", "CAPTAIN", "EXPLORE", "FREEDOM", "HORIZON", "KINGDOM",
        "LEOPARD", "MORNING", "NETWORK", "OUTLOOK", "PENGUIN", "RAINBOW", "SAILING",
        "THUNDER", "UNICORN", "VOLCANO", "WARRIOR"
    )

    private val puzzles = listOf(
        Puzzle("Animals", "ROOSTER", "Male chicken"),
        Puzzle("Animals", "DOLPHIN", "Intelligent sea mammal"),
        Puzzle("Colors", "VIOLET", "Red plus blue"),
        Puzzle("Colors", "INDIGO", "Between blue and violet"),
        Puzzle("Nature", "SPRINGTIME", "Season of flowers"),
        Puzzle("Arts", "MUSIC", "Art of sound and melody"),
        Puzzle("Food", "CHOCOLATE", "Made from cacao beans")
    )

    @Test
    fun `generated boards always satisfy verify across every difficulty and puzzle`() {
        var successes = 0
        var total = 0
        for (difficulty in Difficulty.STANDARD) {
            val gridSize = WordSearchGenerator.gridSizeFor(difficulty)
            val maxWordLen = gridSize.coerceAtMost(12)
            for (puzzle in puzzles) {
                total++
                val words = enWords.filter { it.length in 4..maxWordLen && it != puzzle.hiddenWord }
                val board = WordSearchGenerator.generate(words, puzzle, difficulty)
                if (board != null) {
                    successes++
                    assertTrue(
                        "Board for ${puzzle.hiddenWord}/$difficulty failed verify()",
                        board.verify()
                    )
                    assertEqualsHiddenWord(puzzle.hiddenWord, board)
                }
            }
        }
        // Not every combination is guaranteed to converge, but the generator must succeed
        // often enough — and must NEVER return an unverified board.
        assertTrue("Generator succeeded on $successes/$total combinations, expected majority", successes * 2 >= total)
    }

    @Test
    fun `all hardcoded fallback boards pass verify`() {
        val boards = listOf(
            FallbackPuzzles.enSize8, FallbackPuzzles.enSize10, FallbackPuzzles.enSize12,
            FallbackPuzzles.enSize14, FallbackPuzzles.enSize16,
            FallbackPuzzles.frSize8, FallbackPuzzles.frSize10, FallbackPuzzles.frSize12,
            FallbackPuzzles.frSize14, FallbackPuzzles.frSize16
        )
        for (board in boards) {
            assertTrue("Fallback board '${board.theme}' (${board.rows}x${board.cols}) failed verify()", board.verify())
        }
    }

    private fun assertEqualsHiddenWord(hiddenWord: String, board: com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard) {
        val spelled = board.hiddenWordCells.joinToString("") { (r, c) -> board.grid[r][c].toString() }
        assertTrue("Hidden word cells did not spell '$hiddenWord', got '$spelled'", spelled == hiddenWord)
    }
}
