package com.payfacto.braingames.feature.animals.data

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.animals.engine.AnimalsGenerator
import com.payfacto.braingames.feature.animals.model.AnimalsPuzzle
import kotlin.random.Random

/** Entry point for a new puzzle: generated when possible, from the verified bank otherwise. */
object AnimalsPuzzles {

    /** CPU-bound (a few ms on the JVM, see AnimalsGeneratorTest): call from Dispatchers.Default. */
    fun newPuzzle(difficulty: Difficulty, random: Random = Random.Default): AnimalsPuzzle =
        AnimalsGenerator(random).generate(difficulty)
            ?: AnimalsFallbackPuzzles.random(difficulty, random)
}
