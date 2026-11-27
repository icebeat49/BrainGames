package com.payfacto.braingames.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.payfacto.braingames.core.model.GameType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Picks a puzzle index in a bundled bank at random while avoiding the ones served recently,
 * per game and grid size. The recent list lives in DataStore, so it also holds across app
 * restarts. Keys are `<game>_recent_<size>`, e.g. `crowns_recent_9`.
 */
@Singleton
class RecentPuzzles @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** An index in `0 until count` that is not among the last `windowFor(count)` picks for this bank. */
    suspend fun pick(game: GameType, size: Int, count: Int, random: Random = Random.Default): Int {
        require(count > 0) { "empty $game bank for size $size" }
        val key = stringPreferencesKey(keyFor(game, size))
        val window = windowFor(count)
        var chosen = 0
        dataStore.edit { prefs ->
            val recent = prefs[key].orEmpty().split(',')
                .mapNotNull(String::toIntOrNull)
                .filter { it in 0 until count }
            val blocked = recent.takeLast(window).toSet()
            val candidates = (0 until count).filter { it !in blocked }.ifEmpty { (0 until count).toList() }
            chosen = candidates.random(random)
            prefs[key] = (recent + chosen).takeLast(window).joinToString(",")
        }
        return chosen
    }

    companion object {
        const val MAX_RECENT = 50

        /** How many past picks are avoided: up to 50, and never more than half the bank. */
        fun windowFor(count: Int): Int = minOf(MAX_RECENT, count / 2)

        fun keyFor(game: GameType, size: Int): String = "${game.name.lowercase()}_recent_$size"
    }
}
