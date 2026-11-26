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
        crownsEnabled = BuildConfig.FEATURE_CROWNS
    )

    fun isEnabled(game: GameType): Boolean = game in enabledGames

    /**
     * Sound effects. When off, nothing plays and the mute controls are hidden;
     * the sound code and the saved mute setting are kept for when it comes back.
     */
    val isSoundEnabled: Boolean = BuildConfig.FEATURE_SOUND

    /** Exhaustive on purpose: a new [GameType] must decide here whether it is shown. */
    internal fun enabledGamesFor(wordSearchEnabled: Boolean, crownsEnabled: Boolean): Set<GameType> =
        GameType.entries.filterTo(LinkedHashSet()) { game ->
            when (game) {
                GameType.SUDOKU      -> true
                GameType.KAKURO      -> true
                GameType.WORD_SEARCH -> wordSearchEnabled
                GameType.CROWNS      -> crownsEnabled
            }
        }
}
