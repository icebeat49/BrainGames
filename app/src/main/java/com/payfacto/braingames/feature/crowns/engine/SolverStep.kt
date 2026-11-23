package com.payfacto.braingames.feature.crowns.engine

import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle

/** Deduction techniques, from easiest to hardest. The ordinal is the difficulty. */
enum class Technique {
    /** A row, column or region has a single possible cell: the crown goes there. */
    SINGLE_CANDIDATE,

    /**
     * A region's candidates all lie in one row (or column): the rest of that row is
     * crossed out. And the reverse: a row's (or column's) candidates all lie in one
     * region, so the rest of that region is crossed out.
     */
    CONFINED_AREA,

    /**
     * A cell is crossed out because a crown there would leave some row, column or
     * region with no possible cell: it touches, or shares a line or region with, all of
     * that area's candidates.
     */
    NEIGHBOUR_ELIMINATION,

    /**
     * k regions whose candidates all fit in k rows (or columns) own those rows: the rest
     * of the rows is crossed out. And the reverse, k rows (or columns) inside k regions.
     */
    GROUP
}

enum class AreaType { ROW, COLUMN, REGION }

/** A row, column or region, by index. */
data class Area(val type: AreaType, val index: Int)

fun CrownsPuzzle.cellsOf(area: Area): List<Int> = when (area.type) {
    AreaType.ROW -> rowCells[area.index]
    AreaType.COLUMN -> columnCells[area.index]
    AreaType.REGION -> regionCells[area.index]
}

/**
 * One deduction, as data for hints. The UI turns it into a localized sentence.
 *
 * - SINGLE_CANDIDATE: [placements] has one cell; [sources] is the area with one candidate.
 * - CONFINED_AREA: [sources] is the confined area, [targets] the line or region cleared.
 * - NEIGHBOUR_ELIMINATION: [sources] is the area those cells would empty.
 * - GROUP: [sources] are the k confined areas, [targets] the k areas they own.
 */
data class SolverStep(
    val technique: Technique,
    val placements: Set<Int> = emptySet(),
    val eliminations: Set<Int> = emptySet(),
    val sources: List<Area> = emptyList(),
    val targets: List<Area> = emptyList()
)
