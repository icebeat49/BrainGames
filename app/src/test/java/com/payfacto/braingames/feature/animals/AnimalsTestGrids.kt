package com.payfacto.braingames.feature.animals

import com.payfacto.braingames.feature.animals.model.AnimalsPuzzle

/** Hand-made grids. Rows of region letters ('A' = region 0); solution as one column digit per row. */
object AnimalsTestGrids {

    fun parse(solution: String, vararg rows: String): AnimalsPuzzle = AnimalsPuzzle(
        size = rows.size,
        regions = rows.joinToString("").map { it - 'A' }.toIntArray(),
        solution = solution.map { it - '0' }.toIntArray()
    )

    /**
     * Unique and solvable by logic. Region A is a single cell; after that the solver needs
     * single candidates, confinement and one neighbour elimination (cell (2,2) would touch
     * or share a row with both of region D's candidates).
     */
    val uniqueFiveByFive: AnimalsPuzzle = parse(
        "02413",
        "ABBCC",
        "BBBCC",
        "DDBCC",
        "DDEEC",
        "DEEEE",
    )

    /** Every row is its own region: any non-touching permutation works, so many solutions. */
    val rowsAsRegions: AnimalsPuzzle = parse(
        "02413",
        "AAAAA",
        "BBBBB",
        "CCCCC",
        "DDDDD",
        "EEEEE",
    )
}
