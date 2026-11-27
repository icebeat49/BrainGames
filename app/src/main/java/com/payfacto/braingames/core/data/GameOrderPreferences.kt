package com.payfacto.braingames.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.payfacto.braingames.core.model.GameType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The order of the games on Home, as the player arranged it. */
@Singleton
class GameOrderPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** Every game, in the saved order; games never placed (e.g. added in an update) follow at the end. */
    val order: Flow<List<GameType>> = dataStore.data.map { orderFrom(it[KEY]) }

    suspend fun setOrder(order: List<GameType>) {
        dataStore.edit { it[KEY] = order.joinToString(SEPARATOR) { game -> game.name } }
    }

    companion object {
        private val KEY = stringPreferencesKey("home_game_order")
        private const val SEPARATOR = ","

        /** Parses a saved order: unknown or repeated names are skipped, missing games appended. */
        fun orderFrom(saved: String?): List<GameType> {
            val placed = saved.orEmpty().split(SEPARATOR)
                .mapNotNull { name -> GameType.entries.firstOrNull { it.name == name } }
                .distinct()
            return placed + GameType.entries.filterNot { it in placed }
        }
    }
}
