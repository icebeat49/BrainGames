package com.payfacto.braingames.feature.crowns.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Picks a puzzle index at random while avoiding the ones served recently, per grid size.
 * The recent list lives in DataStore, so it also holds across app restarts.
 */
@Singleton
class RecentCrownsPuzzles @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** An index in `0 until count` that is not among the last `windowFor(count)` picks for [size]. */
    suspend fun pick(size: Int, count: Int, random: Random = Random.Default): Int {
        require(count > 0) { "empty bank for size $size" }
        val key = stringPreferencesKey("crowns_recent_$size")
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
    }
}
