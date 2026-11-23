package com.payfacto.braingames.feature.crowns.data

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.engine.CrownsDifficulty
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Supplies new Crowns puzzles. An interface so ViewModel tests can use a fixed grid. */
interface CrownsPuzzleSource {
    suspend fun newPuzzle(difficulty: Difficulty): CrownsPuzzle
}

/**
 * Serves puzzles from the bundled bank, at random but never one of the recent ones for that
 * size. Instant on any phone, unlike generating 10×10 or 11×11 puzzles on the device.
 */
class BundledCrownsPuzzleSource @Inject constructor(
    private val bank: CrownsPuzzleBank,
    private val recent: RecentCrownsPuzzles
) : CrownsPuzzleSource {

    override suspend fun newPuzzle(difficulty: Difficulty): CrownsPuzzle = withContext(Dispatchers.IO) {
        val size = CrownsDifficulty.sizeFor(difficulty)
        val puzzles = bank.puzzles(size)
        puzzles[recent.pick(size, puzzles.size)]
    }
}
