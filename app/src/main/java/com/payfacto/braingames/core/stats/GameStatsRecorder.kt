package com.payfacto.braingames.core.stats

import androidx.lifecycle.SavedStateHandle

/**
 * Counts one puzzle in the stats: played on its first move, won once. The flags live in
 * the SavedStateHandle and in the disk save ([KEYS]), so rotating, process death or
 * Continue never count the same puzzle twice.
 */
class GameStatsRecorder(
    private val key: StatsKey,
    private val handle: SavedStateHandle,
    private val stats: StatsStore
) {
    /** Call on every player change; only the first one of a puzzle counts. */
    fun onMove() {
        if (handle.get<Boolean>(KEY_PLAYED) == true) return
        handle[KEY_PLAYED] = true
        stats.recordPlayed(key)
    }

    /** A win is also a game played, even if it took no counted move. */
    fun onWin(elapsedMs: Long) {
        if (handle.get<Boolean>(KEY_WON) == true) return
        onMove()
        handle[KEY_WON] = true
        stats.recordWin(key, elapsedMs)
    }

    /** A new puzzle starts uncounted. */
    fun onNewPuzzle() {
        KEYS.forEach { handle.remove<Boolean>(it) }
    }

    companion object {
        private const val KEY_PLAYED = "stats_played"
        private const val KEY_WON = "stats_won"

        /** Entries the disk save must keep with the game. */
        val KEYS = listOf(KEY_PLAYED, KEY_WON)
    }
}
