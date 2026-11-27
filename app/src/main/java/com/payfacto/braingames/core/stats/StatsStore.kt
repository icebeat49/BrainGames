package com.payfacto.braingames.core.stats

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Which stats a game counts towards: one game, at one level, in one mode. */
data class StatsKey(val game: GameType, val difficulty: Difficulty, val mode: GameMode)

/** The record for one [StatsKey], or a sum of several. Times are in milliseconds. */
data class GameStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestMs: Long? = null,
    val totalWonMs: Long = 0L
) {
    /** Average time of the games won; null before the first win. */
    val averageMs: Long? get() = if (won == 0) null else totalWonMs / won

    operator fun plus(other: GameStats) = GameStats(
        played = played + other.played,
        won = won + other.won,
        bestMs = listOfNotNull(bestMs, other.bestMs).minOrNull(),
        totalWonMs = totalWonMs + other.totalWonMs
    )
}

/**
 * Played and won counts, best and total winning time, per game × level × mode, in their own
 * DataStore file. Kept apart from the saves, which are deleted on every win: stats change
 * only when a game is played or the player resets them.
 *
 * Writes run on [scope], which outlives screens, so a win is recorded even when the player
 * leaves right after it.
 */
class StatsStore(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope
) {
    /** Every key with something recorded. */
    val all: Flow<Map<StatsKey, GameStats>> = dataStore.data.map { prefs ->
        allKeys().associateWith { prefs.statsFor(it) }.filterValues { it != GameStats() }
    }

    /** Counts a game as played: called once per puzzle, on its first move. */
    fun recordPlayed(key: StatsKey) = update { prefs ->
        prefs[playedKey(key)] = (prefs[playedKey(key)] ?: 0) + 1
    }

    fun recordWin(key: StatsKey, elapsedMs: Long) = update { prefs ->
        prefs[wonKey(key)] = (prefs[wonKey(key)] ?: 0) + 1
        prefs[totalKey(key)] = (prefs[totalKey(key)] ?: 0L) + elapsedMs
        val best = prefs[bestKey(key)]
        if (best == null || elapsedMs < best) prefs[bestKey(key)] = elapsedMs
    }

    /** Clears one game, at every level and mode. */
    fun reset(game: GameType) = update { prefs ->
        allKeys().filter { it.game == game }.forEach { prefs.clear(it) }
    }

    fun resetAll() = update { prefs -> allKeys().forEach { prefs.clear(it) } }

    private fun update(change: (MutablePreferences) -> Unit) {
        scope.launch { dataStore.edit(change) }
    }

    private fun Preferences.statsFor(key: StatsKey) = GameStats(
        played = this[playedKey(key)] ?: 0,
        won = this[wonKey(key)] ?: 0,
        bestMs = this[bestKey(key)],
        totalWonMs = this[totalKey(key)] ?: 0L
    )

    private fun MutablePreferences.clear(key: StatsKey) {
        remove(playedKey(key))
        remove(wonKey(key))
        remove(bestKey(key))
        remove(totalKey(key))
    }

    private fun allKeys(): List<StatsKey> = GameType.entries.flatMap { game ->
        Difficulty.entries.flatMap { difficulty -> GameMode.entries.map { StatsKey(game, difficulty, it) } }
    }

    private fun prefix(key: StatsKey) =
        "${key.game.name.lowercase()}_${key.difficulty.name.lowercase()}_${key.mode.name.lowercase()}"

    private fun playedKey(key: StatsKey) = intPreferencesKey("${prefix(key)}_played")
    private fun wonKey(key: StatsKey) = intPreferencesKey("${prefix(key)}_won")
    private fun bestKey(key: StatsKey) = longPreferencesKey("${prefix(key)}_best_ms")
    private fun totalKey(key: StatsKey) = longPreferencesKey("${prefix(key)}_total_won_ms")
}
