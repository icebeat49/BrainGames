package com.payfacto.braingames.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.payfacto.braingames.core.model.GameType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Whether a game's "How to play" was already seen, so it opens by itself only the first time. */
@Singleton
class HowToPlayPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    fun hasSeen(game: GameType): Flow<Boolean> = dataStore.data.map { it[keyFor(game)] ?: false }

    suspend fun markSeen(game: GameType) {
        dataStore.edit { it[keyFor(game)] = true }
    }

    private fun keyFor(game: GameType) = booleanPreferencesKey("${game.name.lowercase()}_how_to_play_seen")
}
