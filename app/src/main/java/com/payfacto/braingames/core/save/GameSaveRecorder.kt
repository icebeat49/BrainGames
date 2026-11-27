package com.payfacto.braingames.core.save

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.session.GameSession

/**
 * Copies a game's SavedStateHandle entries to disk and back, so a game can be continued
 * after the app was closed. A game keeps using its SavedStateHandle as before; it calls
 * [record] after each change, [clear] on a new puzzle or a win, and [restoreInto] when
 * the player chose Continue.
 *
 * [keys] are the game's own saver keys; [GameSession]'s keys are always included.
 */
class GameSaveRecorder(
    private val game: GameType,
    private val handle: SavedStateHandle,
    private val saves: GameSaves,
    private val difficulty: Difficulty,
    private val mode: GameMode,
    keys: List<String>
) {
    private val keys = keys + GameSession.SAVED_KEYS

    /** Saves the current game, but only while it is being played: not before it loads, nor once it is won. */
    fun record() {
        if (!GameSession.isPlayingIn(handle)) return
        val values = keys.mapNotNull { key -> handle.get<Any>(key)?.let { key to copyOf(it) } }.toMap()
        saves.save(game, GameSave(difficulty, mode, values))
    }

    fun clear() = saves.clear(game)

    /**
     * Copies the saved game into [handle]. Returns false, and changes nothing, when there is
     * no save for this difficulty and mode.
     */
    suspend fun restoreInto(): Boolean {
        val save = saves.load(game) ?: return false
        if (save.difficulty != difficulty || save.mode != mode) return false
        save.values.forEach { (key, value) -> if (key in keys) handle[key] = copyOf(value) }
        return true
    }

    /** Arrays are copied so a later in-place change can't alter a save already handed over. */
    private fun copyOf(value: Any): Any = when (value) {
        is IntArray -> value.copyOf()
        is BooleanArray -> value.copyOf()
        else -> value
    }
}
