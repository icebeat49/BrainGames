package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.crowns.engine.CrownsGenerationRules
import com.payfacto.braingames.feature.crowns.engine.RegionLayout
import com.payfacto.braingames.feature.crowns.engine.RegionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RegionLayoutTest {

    private val hardRules = CrownsGenerationRules.forDifficulty(Difficulty.HARD)

    @Test
    fun `balanced growth plus small-region fixing always ends within the rules`() {
        repeat(LAYOUTS) { seed ->
            val random = Random(seed)
            val layout = grownLayout(random, hardRules, singleCellRegion = null)

            assertTrue("layout $seed", layout.isShapeAllowed)
            assertTrue("layout $seed", (0 until SIZE).all { layout.sizeOf(it) >= RegionLayout.MIN_REGION_SIZE })
        }
    }

    @Test
    fun `the planned single-cell region stays a single cell`() {
        val rules = CrownsGenerationRules.forDifficulty(Difficulty.VERY_EASY)
        repeat(LAYOUTS) { seed ->
            val random = Random(seed)
            val layout = grownLayout(random, rules, singleCellRegion = 3)

            assertEquals("layout $seed", 1, layout.sizeOf(3))
            assertEquals("layout $seed", 1, (0 until SIZE).count { layout.sizeOf(it) == 1 })
        }
    }

    @Test
    fun `repair moves never take a region below two cells or add too many two-cell regions`() {
        val random = Random(7)
        val layout = grownLayout(random, hardRules, singleCellRegion = null)

        repeat(MOVES) { layout.moveToNeighbourRegion(random.nextInt(SIZE * SIZE), random) }

        assertTrue(layout.isShapeAllowed)
    }

    private fun grownLayout(random: Random, rules: RegionRules, singleCellRegion: Int?): RegionLayout {
        // One seed per row, columns 0,2,4,6,1,3,5: no two touch.
        val seeds = intArrayOf(0, 2, 4, 6, 1, 3, 5).mapIndexed { row, col -> row * SIZE + col }.toIntArray()
        return RegionLayout(SIZE, seeds, rules, singleCellRegion).apply {
            assertTrue(growBalanced(random))
            assertTrue(fixSmallRegions(random))
        }
    }

    private companion object {
        const val SIZE = 7
        const val LAYOUTS = 200
        const val MOVES = 500
    }
}
