package com.payfacto.braingames.feature.crowns.data

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.engine.CrownsGenerator
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import kotlin.random.Random

/** Entry point for a new puzzle: generated when possible, from the verified bank otherwise. */
object CrownsPuzzles {

    /** CPU-bound (a few ms on the JVM, see CrownsGeneratorTest): call from Dispatchers.Default. */
    fun newPuzzle(difficulty: Difficulty, random: Random = Random.Default): CrownsPuzzle =
        CrownsGenerator(random).generate(difficulty)
            ?: CrownsFallbackPuzzles.random(difficulty, random)
}
