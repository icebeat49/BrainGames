package com.payfacto.braingames.feature.crowns.data

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import kotlin.random.Random

/**
 * Pre-verified puzzles used only if [com.payfacto.braingames.feature.crowns.engine.CrownsGenerator]
 * gives up, which did not happen once in testing. Produced by the generator itself
 * (CrownsFallbackPrinter in the unit tests) and re-checked by CrownsFallbackPuzzlesTest:
 * valid regions, unique solution, solvable by logic, at their difficulty.
 *
 * Each entry: the solution as one column letter per row ('A' = column 0, so 11 columns fit),
 * then one string per row in which each letter is a region ('A' = region 0).
 */
object CrownsFallbackPuzzles {

    fun random(difficulty: Difficulty, random: Random = Random.Default): CrownsPuzzle =
        bank.getValue(difficulty).random(random)

    fun all(difficulty: Difficulty): List<CrownsPuzzle> = bank.getValue(difficulty)

    private fun puzzle(solution: String, vararg rows: String) = CrownsPuzzle(
        size = rows.size,
        regions = rows.joinToString("").map { it - 'A' }.toIntArray(),
        solution = solution.map { it - 'A' }.toIntArray()
    )

    private val bank: Map<Difficulty, List<CrownsPuzzle>> by lazy {
        mapOf(
            Difficulty.VERY_EASY to listOf(
                puzzle("CAFBDGE", "ABBBGGG", "AAABBGG", "AAABDGG", "ADDDDGG", "DDFCDGG", "DDFFFGE", "DDDFFEE"),
                puzzle("DFBGEAC", "AAAABBB", "CAAGBBF", "CCCGFFF", "CCGGFFF", "CGGGGFF", "EGDDDFF", "DDDFFFF"),
                puzzle("DGBFACE", "GGGGAAA", "GGGAAEA", "GBEAAEE", "GEEEEEE", "FFFFFEE", "FDDFCCE", "FDCCCEE"),
            ),
            Difficulty.EASY to listOf(
                puzzle("HDBEGACF", "CAAAAAFB", "CAAAEFFF", "CCEEEFFF", "EEEEEEEF", "EEEHHEFF", "DGEEHEEF", "DGGEHHFF", "DEEEEHHF"),
                puzzle("HEACGDBF", "CCEEEEEB", "CCDDEEEE", "CDDDDDFE", "GGDHDFFF", "GGDHDHFF", "GHHHDHHA", "GGGHHHAA", "GGHHHAAA"),
                puzzle("CEADFHBG", "AAAAAEEE", "ABAAEEEE", "FBAAHHHH", "CBABHHHH", "CBBBDDDH", "CBBHHHHH", "CCBBHCCH", "CCCCCCGH"),
            ),
            Difficulty.MEDIUM to listOf(
                puzzle("GBFAHECID", "CCCCCDEEE", "CCIIIDDEG", "HHIIIDDEG", "HIIIDDDGG", "BBIIIIIGI", "BBBIIIIII", "BBBIAIIII", "BBBIAIIFF", "BBBAAFFFF"),
                puzzle("DHEBFIAGC", "DDDDDHHHH", "GDDDDHHHH", "GGGGICHHH", "GGCCCCHHH", "GGCCCCHHH", "GGCCCCCCB", "ECCCCACBB", "EFCCAAABB", "FFFAAAABB"),
                puzzle("BDIAFCHEG", "DDDDDIIII", "DDHHDHHHI", "HHHDDHHHI", "CHHHHHIII", "CHHHHBFIF", "CGEHFFFFF", "CGGGFFFFF", "CCAGGGAFF", "CCAAAAAFF"),
            ),
            Difficulty.HARD to listOf(
                puzzle("DJECGAFHBI", "IIIIHHDDDD", "IIIIIHHHHD", "JJJCCHHHHH", "JJJJJHHHHH", "BJEEHHHHHH", "BJEHHHHAHH", "BBEEEEHAAH", "GBEEEHHAHH", "GGEEEHAAFF", "GGGEEHAFFF"),
                puzzle("BEHDIAFCJG", "GGAAAAABBB", "GAAAACCCBB", "GAAAACCBBB", "EFFDCCCCCJ", "EEFFCCHHCJ", "EFFFCHHJJJ", "FFFFHHIIIJ", "FFFHHIIIIJ", "FFHHHIIIJJ", "FFHHHIIIII"),
                puzzle("EJAHDFBGCI", "CEEEEEEEEE", "CEAAEAEAEF", "CCAAAAAAAA", "GCCGGDDADA", "GGCGGGDDDD", "GGGGDDDDDJ", "GIBGGDJJDJ", "GBBJDDJDDJ", "GGBJJJJJJJ", "GGBBJJJHHJ"),
            ),
            Difficulty.EXPERT to listOf(
                puzzle("JFAECGIKDHB", "CCCHHDDDDAA", "GGCCHDHHDDA", "GGCCHHHHHDA", "CGCCHHJJJJA", "CCCCJJJJJJK", "CCJJJIJKJKK", "CCFFJIJKKKK", "CCFJJIIIKKB", "FFFFJIIKKKI", "FFFFFFIIIII", "FEFFIIIIIII"),
                puzzle("BKIAEJDHFCG", "JJJJJJJJHHH", "JJDJJDJHHKK", "DJDDDDJJHHH", "DJDDDEEEEAI", "DDDDEEAAAAI", "DDFEEEAIAII", "DDFFFEAIIII", "FFFFFEABIII", "FFAAFAABIBB", "FFGAAAABBBB", "GGGGGACCCBB"),
                puzzle("ECJGIBHFDKA", "EEEEEEEAAAA", "KKKEEEAAAAA", "KKEEHAAAAAA", "KKKHHHBBCCA", "KKKHHHIBCAA", "KDKKKHIICAJ", "DDDHHHJIAAJ", "HHDHJJJIJJJ", "HHHHHHJJJJJ", "HHHHJJJGGJG", "FHHHHHJGGGG"),
            ),
        )
    }
}
