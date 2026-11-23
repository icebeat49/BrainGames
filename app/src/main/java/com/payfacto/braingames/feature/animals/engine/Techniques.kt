package com.payfacto.braingames.feature.animals.engine

/**
 * One finder per [Technique]. Each looks for a single deduction that changes the grid
 * and returns it without applying it, or null when the technique finds nothing.
 */
internal object Techniques {

    fun find(grid: CandidateGrid, technique: Technique): SolverStep? = when (technique) {
        Technique.SINGLE_CANDIDATE -> singleCandidate(grid)
        Technique.CONFINED_AREA -> confinedArea(grid)
        Technique.NEIGHBOUR_ELIMINATION -> neighbourElimination(grid)
        Technique.GROUP -> group(grid)
    }

    /** (a) An area without an animal and with exactly one candidate. Regions first, as players scan them first. */
    private fun singleCandidate(grid: CandidateGrid): SolverStep? {
        for (area in grid.areasRegionsFirst()) {
            if (grid.hasAnimalIn(area)) continue
            val candidates = grid.candidatesOf(area)
            if (candidates.size == 1) {
                return SolverStep(Technique.SINGLE_CANDIDATE, placements = setOf(candidates.single()), sources = listOf(area))
            }
        }
        return null
    }

    /**
     * (b) Region → line: a region's candidates in a single row or column clear the rest of it.
     * Line → region: a row's or column's candidates in a single region clear the rest of the region.
     */
    private fun confinedArea(grid: CandidateGrid): SolverStep? {
        for (source in grid.areasRegionsFirst()) {
            if (grid.hasAnimalIn(source)) continue
            val candidates = grid.candidatesOf(source)
            if (candidates.isEmpty()) continue
            val targetTypes = if (source.type == AreaType.REGION) {
                listOf(AreaType.ROW, AreaType.COLUMN)
            } else {
                listOf(AreaType.REGION)
            }
            for (targetType in targetTypes) {
                val target = grid.commonArea(candidates, targetType) ?: continue
                val sourceCells = grid.cellsOf(source).toHashSet()
                val eliminations = grid.candidatesOf(target).filterTo(LinkedHashSet()) { it !in sourceCells }
                if (eliminations.isNotEmpty()) {
                    return SolverStep(
                        Technique.CONFINED_AREA,
                        eliminations = eliminations,
                        sources = listOf(source),
                        targets = listOf(target)
                    )
                }
            }
        }
        return null
    }

    /** (c) Cells that would leave some area with no candidate at all, grouped by that area. */
    private fun neighbourElimination(grid: CandidateGrid): SolverStep? {
        val puzzle = grid.puzzle
        for (area in grid.areasRegionsFirst()) {
            if (grid.hasAnimalIn(area)) continue
            val areaCandidates = grid.candidatesOf(area)
            if (areaCandidates.isEmpty()) continue
            val areaCells = grid.cellsOf(area).toHashSet()
            val eliminations = (0 until puzzle.cellCount).filterTo(LinkedHashSet()) { cell ->
                grid.isCandidate(cell) && cell !in areaCells &&
                    areaCandidates.all { puzzle.excludes(cell, it) }
            }
            if (eliminations.isNotEmpty()) {
                return SolverStep(Technique.NEIGHBOUR_ELIMINATION, eliminations = eliminations, sources = listOf(area))
            }
        }
        return null
    }

    /**
     * (d) k regions whose candidates span exactly k rows (or columns): those lines belong to
     * them, so the lines' other candidates go. And k rows (or columns) spanning exactly k
     * regions: those regions' other candidates go. Smallest groups first; k = 1 is (b).
     */
    private fun group(grid: CandidateGrid): SolverStep? {
        val pairings = listOf(
            AreaType.REGION to AreaType.ROW,
            AreaType.REGION to AreaType.COLUMN,
            AreaType.ROW to AreaType.REGION,
            AreaType.COLUMN to AreaType.REGION,
        )
        val open = AreaType.entries.associateWith { type ->
            List(grid.puzzle.size) { Area(type, it) }.filter { !grid.hasAnimalIn(it) }
        }
        val maxGroup = open.getValue(AreaType.REGION).size - 1
        for (k in 2..maxGroup) {
            for ((sourceType, targetType) in pairings) {
                for (sources in combinations(open.getValue(sourceType), k)) {
                    val step = groupStep(grid, sources, targetType, k)
                    if (step != null) return step
                }
            }
        }
        return null
    }

    private fun groupStep(grid: CandidateGrid, sources: List<Area>, targetType: AreaType, k: Int): SolverStep? {
        val candidates = sources.flatMap(grid::candidatesOf)
        val targets = candidates.map { grid.areaOf(it, targetType) }.distinct()
        if (targets.size != k) return null
        val sourceCells = sources.flatMapTo(HashSet(), grid::cellsOf)
        val eliminations = targets.flatMap(grid::candidatesOf).filterTo(LinkedHashSet()) { it !in sourceCells }
        if (eliminations.isEmpty()) return null
        return SolverStep(
            Technique.GROUP,
            eliminations = eliminations,
            sources = sources,
            targets = targets.sortedBy { it.index }
        )
    }

    private fun CandidateGrid.areasRegionsFirst(): List<Area> = areas.sortedBy {
        when (it.type) {
            AreaType.REGION -> 0
            AreaType.ROW -> 1
            AreaType.COLUMN -> 2
        }
    }

    /** The single area of [type] containing all [cells], or null if they span several. */
    private fun CandidateGrid.commonArea(cells: List<Int>, type: AreaType): Area? =
        cells.map { areaOf(it, type) }.distinct().singleOrNull()

    private fun CandidateGrid.areaOf(cell: Int, type: AreaType): Area = when (type) {
        AreaType.ROW -> Area(type, puzzle.rowOf(cell))
        AreaType.COLUMN -> Area(type, puzzle.columnOf(cell))
        AreaType.REGION -> Area(type, puzzle.regionOf(cell))
    }

    /** All k-element subsets of [items], in order. */
    private fun <T> combinations(items: List<T>, k: Int): Sequence<List<T>> = sequence {
        val indices = IntArray(k) { it }
        if (k > items.size) return@sequence
        while (true) {
            yield(indices.map { items[it] })
            var i = k - 1
            while (i >= 0 && indices[i] == items.size - k + i) i--
            if (i < 0) return@sequence
            indices[i]++
            for (j in i + 1 until k) indices[j] = indices[j - 1] + 1
        }
    }
}
