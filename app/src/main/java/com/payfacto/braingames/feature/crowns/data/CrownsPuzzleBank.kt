package com.payfacto.braingames.feature.crowns.data

import android.content.Context
import com.payfacto.braingames.R
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The bundled, pre-verified puzzles for each grid size, read once from res/raw. Generated
 * offline by CrownsBankWriter (unit tests) because 10×10 and 11×11 are too slow to generate
 * on a phone; CrownsBankTest re-verifies every puzzle.
 */
@Singleton
class CrownsPuzzleBank @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = ConcurrentHashMap<Int, List<CrownsPuzzle>>()

    /** Blocking file read on first use per size: call off the main thread. */
    fun puzzles(size: Int): List<CrownsPuzzle> = cache.getOrPut(size) {
        val text = context.resources.openRawResource(rawIdFor(size)).bufferedReader().use { it.readText() }
        CrownsBankFormat.parse(text)
    }

    private fun rawIdFor(size: Int): Int = when (size) {
        7 -> R.raw.crowns_bank_7
        8 -> R.raw.crowns_bank_8
        9 -> R.raw.crowns_bank_9
        10 -> R.raw.crowns_bank_10
        11 -> R.raw.crowns_bank_11
        else -> error("No Crowns bank for size $size")
    }
}
