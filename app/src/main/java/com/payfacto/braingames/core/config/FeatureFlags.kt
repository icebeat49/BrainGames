package com.payfacto.braingames.core.config

import com.payfacto.braingames.BuildConfig
import com.payfacto.braingames.core.model.GameType

/**
 * Which games users can reach. Hidden games keep all their code, routes and data;
 * they are only left off the Home screen, and their routes pop straight back.
 */
object FeatureFlags {

    val enabledGames: Set<GameType> = enabledGamesFor(
        wordSearchEnabled = BuildConfig.FEATURE_WORD_SEARCH,
        animalsEnabled = BuildConfig.FEATURE_ANIMALS
    )

    fun isEnabled(game: GameType): Boolean = game in enabledGames

    /** Exhaustive on purpose: a new [GameType] must decide here whether it is shown. */
    internal fun enabledGamesFor(wordSearchEnabled: Boolean, animalsEnabled: Boolean): Set<GameType> =
        GameType.entries.filterTo(LinkedHashSet()) { game ->
            when (game) {
                GameType.SUDOKU      -> true
                GameType.KAKURO      -> true
                GameType.WORD_SEARCH -> wordSearchEnabled
                GameType.ANIMALS     -> animalsEnabled
            }
        }
}
