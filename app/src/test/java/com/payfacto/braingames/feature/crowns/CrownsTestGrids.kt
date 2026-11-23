package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle

/** Hand-made grids. Rows of region letters ('A' = region 0); solution as one column digit per row. */
object CrownsTestGrids {

    fun parse(solution: String, vararg rows: String): CrownsPuzzle = CrownsPuzzle(
        size = rows.size,
        regions = rows.joinToString("").map { it - 'A' }.toIntArray(),
        solution = solution.map { it - '0' }.toIntArray()
    )

    /**
     * Unique and solvable by logic. Region A is a single cell; after that the solver needs
     * single candidates, confinement and one neighbour elimination (cell (2,2) would touch
     * or share a row with both of region D's candidates).
     */
    val uniqueFiveByFive: CrownsPuzzle = parse(
        "02413",
        "ABBCC",
        "BBBCC",
        "DDBCC",
        "DDEEC",
        "DEEEE",
    )

    /** Every row is its own region: any non-touching permutation works, so many solutions. */
    val rowsAsRegions: CrownsPuzzle = parse(
        "02413",
        "AAAAA",
        "BBBBB",
        "CCCCC",
        "DDDDD",
        "EEEEE",
    )
}
