package com.payfacto.braingames.feature.crowns.data

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Supplies new Crowns puzzles. An interface so ViewModel tests can use a fixed grid. */
interface CrownsPuzzleSource {
    suspend fun newPuzzle(difficulty: Difficulty): CrownsPuzzle
}

/** Generates on Dispatchers.Default; falls back to the verified bank if generation gives up. */
class GeneratedCrownsPuzzleSource @Inject constructor() : CrownsPuzzleSource {
    override suspend fun newPuzzle(difficulty: Difficulty): CrownsPuzzle =
        withContext(Dispatchers.Default) { CrownsPuzzles.newPuzzle(difficulty) }
}
