package com.payfacto.braingames.feature.animals.data

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.animals.model.AnimalsPuzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Supplies new Animals puzzles. An interface so ViewModel tests can use a fixed grid. */
interface AnimalsPuzzleSource {
    suspend fun newPuzzle(difficulty: Difficulty): AnimalsPuzzle
}

/** Generates on Dispatchers.Default; falls back to the verified bank if generation gives up. */
class GeneratedAnimalsPuzzleSource @Inject constructor() : AnimalsPuzzleSource {
    override suspend fun newPuzzle(difficulty: Difficulty): AnimalsPuzzle =
        withContext(Dispatchers.Default) { AnimalsPuzzles.newPuzzle(difficulty) }
}
