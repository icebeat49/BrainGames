package com.payfacto.braingames.feature.animals.data

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.animals.model.AnimalsPuzzle
import kotlin.random.Random

/**
 * Pre-verified puzzles used only if [com.payfacto.braingames.feature.animals.engine.AnimalsGenerator]
 * gives up, which did not happen once in testing. Produced by the generator itself
 * (AnimalsFallbackPrinter in the unit tests) and re-checked by AnimalsFallbackPuzzlesTest:
 * valid regions, unique solution, solvable by logic, at their difficulty.
 *
 * Each entry: the solution as one column digit per row, then one string per row in which
 * each letter is a region ('A' = region 0).
 */
object AnimalsFallbackPuzzles {

    fun random(difficulty: Difficulty, random: Random = Random.Default): AnimalsPuzzle =
        bank.getValue(difficulty).random(random)

    fun all(difficulty: Difficulty): List<AnimalsPuzzle> = bank.getValue(difficulty)

    private fun puzzle(solution: String, vararg rows: String) = AnimalsPuzzle(
        size = rows.size,
        regions = rows.joinToString("").map { it - 'A' }.toIntArray(),
        solution = solution.map { it - '0' }.toIntArray()
    )

    private val bank: Map<Difficulty, List<AnimalsPuzzle>> by lazy {
        mapOf(
            Difficulty.VERY_EASY to listOf(
                puzzle("03142", "BBDDD", "BBDDD", "BEEDD", "BEAAA", "BBCCA"),
                puzzle("20314", "EECDD", "EDDDA", "EDDDA", "BBDDA", "BAAAA"),
                puzzle("42031", "DDDDC", "DBBBE", "DBBEE", "ABBEE", "AABEE"),
            ),
            Difficulty.EASY to listOf(
                puzzle("251304", "FFFFCC", "AAADDC", "ABADDD", "AADDDE", "ADDEEE", "DDDDEE"),
                puzzle("305241", "BBCCEE", "BAAFFE", "AAADFE", "ADADFF", "DDADFF", "DDDDDD"),
                puzzle("142053", "BBBBEE", "BBBBEE", "BBCEEE", "DBCEFE", "CCCFFF", "CCCAFF"),
            ),
            Difficulty.MEDIUM to listOf(
                puzzle("6253041", "AAAEFFC", "AAAEFFF", "DAEEFFF", "DAEEFFF", "DBBBBFF", "DDBBBFF", "GGBBBBF"),
                puzzle("1352604", "BGDDFFF", "BBDDFFF", "BEEDFFF", "BEEEFFF", "BBEEFFA", "BBBCFFC", "BBBCCCC"),
                puzzle("3514260", "ABBBBCC", "ABBGBCG", "AEGGBGG", "AADGGGG", "AADFFFF", "AAAAAAF", "AAAAAAA"),
            ),
            Difficulty.HARD to listOf(
                puzzle("07142536", "DDHHHFFF", "DHHHHCFF", "DHHHHCCF", "DDAACCCF", "DDAAACBB", "ADAAABBB", "AAAGGBEB", "AAAGGBEB"),
                puzzle("70625314", "BBBBAAAA", "BEEBBAFA", "EEBBBFFA", "EEEBFFFF", "EEBBFDFF", "EEECDDFF", "GGEDDDDF", "GGGGHHDD"),
                puzzle("75361420", "HHGGGGEF", "HHGGEEEF", "HGGGGGEC", "HHGGACCC", "HHHGAAAC", "DHHAAAAC", "DHBAAAAA", "DHHAAAAA"),
            ),
            Difficulty.EXPERT to listOf(
                puzzle("148037526", "GGGGIIIII", "IIGGIICCC", "IIGGGICIC", "FIIIIICII", "FFFHIIIII", "AFHHIDDDD", "AFHEEBDDD", "AFAEEBDDD", "AAAAEEEDD"),
                puzzle("742530681", "BBGGGGGEF", "BBBBGGGGF", "BBBBGIIFF", "BDDDDIFFF", "BDDDDDFFF", "CDDDHHFFF", "CDDDDHHHF", "CCDFFHFFF", "CADDFFFFF"),
                puzzle("635817420", "EIIIIAGGG", "EIIIIAGGG", "EIIICAAFD", "EIEICCAFD", "EEEICCAFD", "HEIICAAFF", "HEICCCCCF", "HHHCHFFCF", "BHHHHHFFF"),
            ),
        )
    }
}
