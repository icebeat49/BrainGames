package com.payfacto.braingames.feature.kakuro.engine

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.kakuro.engine.KakuroTechnique.CONTRADICTION
import com.payfacto.braingames.feature.kakuro.engine.KakuroTechnique.RUN_PERMUTATIONS

/**
 * Difficulty = grid size plus what the logical solve needs. Sizes include the clue row and
 * column. Thresholds were set from the spread of generated puzzles; tune them by play-testing.
 */
object KakuroDifficulty {

    fun sizeFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.VERY_EASY -> 6
        Difficulty.EASY      -> 7
        Difficulty.MEDIUM    -> 8
        Difficulty.HARD      -> 9
        Difficulty.EXPERT, Difficulty.MASTER -> 10
    }

    fun rulesFor(difficulty: Difficulty): KakuroGenerationRules {
        val size = sizeFor(difficulty)
        val whiteShare = when (difficulty) {
            Difficulty.VERY_EASY -> 0.70..0.85
            Difficulty.EASY      -> 0.65..0.80
            Difficulty.MEDIUM    -> 0.60..0.75
            Difficulty.HARD      -> 0.58..0.72
            Difficulty.EXPERT, Difficulty.MASTER -> 0.55..0.70
        }
        return KakuroGenerationRules(size, whiteShare, maxRunLength = minOf(MAX_RUN, size - 1))
    }

    /**
     * Very easy and easy: sums and a few rounds of run permutations, never a trial.
     * Medium: needs run permutations repeatedly, never a trial.
     * Hard: needs at least one trial; expert at least two.
     */
    fun accepts(difficulty: Difficulty, grade: KakuroGrade): Boolean {
        val permutations = grade.roundsOf(RUN_PERMUTATIONS)
        val trials = grade.roundsOf(CONTRADICTION)
        return when (difficulty) {
            Difficulty.VERY_EASY -> trials == 0 && permutations <= 1
            Difficulty.EASY      -> trials == 0 && permutations <= 3
            Difficulty.MEDIUM    -> trials == 0 && permutations >= 2
            Difficulty.HARD      -> trials >= 1
            Difficulty.EXPERT, Difficulty.MASTER -> trials >= 2
        }
    }

    private const val MAX_RUN = 9
}
