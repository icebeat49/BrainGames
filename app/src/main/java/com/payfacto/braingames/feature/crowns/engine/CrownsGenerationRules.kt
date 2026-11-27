package com.payfacto.braingames.feature.crowns.engine

import com.payfacto.braingames.core.model.Difficulty

/**
 * How small regions may be, and how many crowns may fall out immediately, per difficulty.
 *
 * @param singleCellChance chance that a puzzle gets exactly one 1-cell region (never more than one).
 * @param maxTwoCellRegions how many regions of exactly 2 cells a puzzle may have.
 * @param maxImmediateCrowns how many crowns the single-candidate technique alone may place
 *   from the empty board, before any other deduction ([CrownsSolver.immediateCrowns]).
 */
data class RegionRules(
    val singleCellChance: Double,
    val maxTwoCellRegions: Int,
    val maxImmediateCrowns: Int
)

/** Every tuning value of the region rules, in one place. */
object CrownsGenerationRules {
    const val NO_LIMIT = Int.MAX_VALUE

    const val VERY_EASY_SINGLE_CELL_CHANCE = 0.30
    const val EASY_SINGLE_CELL_CHANCE = 0.30
    const val MEDIUM_SINGLE_CELL_CHANCE = 0.15
    const val HARD_SINGLE_CELL_CHANCE = 0.0
    const val EXPERT_SINGLE_CELL_CHANCE = 0.0

    const val VERY_EASY_MAX_TWO_CELL_REGIONS = NO_LIMIT
    const val EASY_MAX_TWO_CELL_REGIONS = NO_LIMIT
    const val MEDIUM_MAX_TWO_CELL_REGIONS = 2
    const val HARD_MAX_TWO_CELL_REGIONS = 1
    const val EXPERT_MAX_TWO_CELL_REGIONS = 1

    const val VERY_EASY_MAX_IMMEDIATE_CROWNS = 3
    const val EASY_MAX_IMMEDIATE_CROWNS = 2
    const val MEDIUM_MAX_IMMEDIATE_CROWNS = 1
    const val HARD_MAX_IMMEDIATE_CROWNS = 1
    const val EXPERT_MAX_IMMEDIATE_CROWNS = 1

    fun forDifficulty(difficulty: Difficulty): RegionRules = when (difficulty) {
        Difficulty.VERY_EASY -> RegionRules(
            VERY_EASY_SINGLE_CELL_CHANCE, VERY_EASY_MAX_TWO_CELL_REGIONS, VERY_EASY_MAX_IMMEDIATE_CROWNS
        )
        Difficulty.EASY -> RegionRules(EASY_SINGLE_CELL_CHANCE, EASY_MAX_TWO_CELL_REGIONS, EASY_MAX_IMMEDIATE_CROWNS)
        Difficulty.MEDIUM -> RegionRules(
            MEDIUM_SINGLE_CELL_CHANCE, MEDIUM_MAX_TWO_CELL_REGIONS, MEDIUM_MAX_IMMEDIATE_CROWNS
        )
        Difficulty.HARD -> RegionRules(HARD_SINGLE_CELL_CHANCE, HARD_MAX_TWO_CELL_REGIONS, HARD_MAX_IMMEDIATE_CROWNS)
        Difficulty.EXPERT, Difficulty.MASTER -> RegionRules(
            EXPERT_SINGLE_CELL_CHANCE, EXPERT_MAX_TWO_CELL_REGIONS, EXPERT_MAX_IMMEDIATE_CROWNS
        )
    }

    /**
     * Whether a finished puzzle's region sizes follow [rules]: at most one 1-cell region, and
     * only when the difficulty allows one at all; at most [RegionRules.maxTwoCellRegions] of 2 cells.
     */
    fun shapeIsAllowed(regionSizes: List<Int>, rules: RegionRules): Boolean {
        val singles = regionSizes.count { it == 1 }
        val maxSingles = if (rules.singleCellChance > 0.0) 1 else 0
        return singles <= maxSingles && regionSizes.count { it == 2 } <= rules.maxTwoCellRegions
    }
}
