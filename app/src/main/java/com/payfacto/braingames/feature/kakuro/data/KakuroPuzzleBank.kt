package com.payfacto.braingames.feature.kakuro.data

import android.content.Context
import com.payfacto.braingames.R
import com.payfacto.braingames.feature.kakuro.engine.KakuroPuzzle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The bundled, pre-verified puzzles for each grid size, read once from res/raw. Generated
 * offline by KakuroBankWriter (unit tests); KakuroBankTest re-verifies every puzzle.
 */
@Singleton
class KakuroPuzzleBank @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = ConcurrentHashMap<Int, List<KakuroPuzzle>>()

    /** Blocking file read on first use per size: call off the main thread. */
    fun puzzles(size: Int): List<KakuroPuzzle> = cache.getOrPut(size) {
        val text = context.resources.openRawResource(rawIdFor(size)).bufferedReader().use { it.readText() }
        KakuroBankFormat.parse(text)
    }

    private fun rawIdFor(size: Int): Int = when (size) {
        6 -> R.raw.kakuro_bank_6
        7 -> R.raw.kakuro_bank_7
        8 -> R.raw.kakuro_bank_8
        9 -> R.raw.kakuro_bank_9
        10 -> R.raw.kakuro_bank_10
        else -> error("No Kakuro bank for size $size")
    }
}
