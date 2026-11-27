package com.payfacto.braingames.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.config.FeatureFlags
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.stats.GameStats
import com.payfacto.braingames.core.stats.StatsKey
import com.payfacto.braingames.core.stats.StatsStore
import com.payfacto.braingames.core.ui.GameCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One game's stats: its total, and per mode one row per level plus that mode's total. */
data class GameStatsSection(
    val game: GameType,
    val total: GameStats,
    val modes: Map<GameMode, ModeStats>
)

data class ModeStats(
    val byDifficulty: Map<Difficulty, GameStats>,
    val total: GameStats
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val store: StatsStore
) : ViewModel() {

    val sections: StateFlow<List<GameStatsSection>> = store.all
        .map { sectionsFor(it, TRACKED_GAMES.filter(FeatureFlags::isEnabled)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = sectionsFor(emptyMap(), TRACKED_GAMES.filter(FeatureFlags::isEnabled))
        )

    fun reset(game: GameType) = store.reset(game)

    fun resetAll() = store.resetAll()

    companion object {
        /** Games that record stats. Hidden Word doesn't yet. */
        val TRACKED_GAMES = listOf(GameType.SUDOKU, GameType.KAKURO, GameType.CROWNS)

        private const val STOP_TIMEOUT_MS = 5_000L

        /** Every level of every mode a game offers, zero where nothing was played. */
        fun sectionsFor(recorded: Map<StatsKey, GameStats>, games: List<GameType>): List<GameStatsSection> =
            games.map { game ->
                val modes = if (GameCatalog.infoFor(game).supportsModes) GameMode.entries else listOf(GameMode.CLASSIC)
                val byMode = modes.associateWith { mode ->
                    val byDifficulty = game.levels.associateWith { difficulty ->
                        recorded[StatsKey(game, difficulty, mode)] ?: GameStats()
                    }
                    ModeStats(byDifficulty, byDifficulty.values.fold(GameStats(), GameStats::plus))
                }
                GameStatsSection(game, byMode.values.fold(GameStats()) { sum, mode -> sum + mode.total }, byMode)
            }
    }
}
