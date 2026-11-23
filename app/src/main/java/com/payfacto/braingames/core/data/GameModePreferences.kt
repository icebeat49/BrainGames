package com.payfacto.braingames.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The last mode chosen, per game. Keys are `<game>_last_mode`, so Sudoku keeps the
 * `sudoku_last_mode` value saved before modes were shared.
 */
@Singleton
class GameModePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    fun lastMode(game: GameType): Flow<GameMode> =
        dataStore.data.map { GameMode.fromName(it[keyFor(game)]) }

    suspend fun setLastMode(game: GameType, mode: GameMode) {
        dataStore.edit { it[keyFor(game)] = mode.name }
    }

    private fun keyFor(game: GameType) = stringPreferencesKey("${game.name.lowercase()}_last_mode")
}
