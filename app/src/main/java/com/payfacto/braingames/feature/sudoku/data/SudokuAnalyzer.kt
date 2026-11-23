package com.payfacto.braingames.feature.sudoku.data

enum class TechniqueLevel {
    NAKED_SINGLE,
    HIDDEN_SINGLE,
    NAKED_PAIR,
    HIDDEN_PAIR,
    POINTING_PAIR,
    NAKED_TRIPLE,
    HIDDEN_TRIPLE,
    X_WING,
    SWORDFISH,
    UNSOLVABLE
}

private val ROWS  = Array(9) { r -> IntArray(9) { c -> r * 9 + c } }
private val COLS  = Array(9) { c -> IntArray(9) { r -> r * 9 + c } }
private val BOXES = Array(9) { b ->
    val br = (b / 3) * 3; val bc = (b % 3) * 3
    IntArray(9) { i -> (br + i / 3) * 9 + bc + i % 3 }
}
private val ALL_UNITS: Array<IntArray> = ROWS + COLS + BOXES
private val PEERS: Array<Set<Int>> = Array(81) { i ->
    val r = i / 9; val c = i % 9; val b = (r / 3) * 3 + c / 3
    (ROWS[r].toList() + COLS[c].toList() + BOXES[b].toList()).filter { it != i }.toSet()
}

private class CandidateGrid(puzzle: IntArray) {
    val values = puzzle.copyOf()
    val cands: Array<MutableSet<Int>> = Array(81) { i ->
        if (puzzle[i] != 0) mutableSetOf() else (1..9).toMutableSet()
    }

    init {
        for (i in 0 until 81) {
            if (values[i] != 0) eliminatePeers(i, values[i])
        }
    }

    fun place(i: Int, v: Int) {
        values[i] = v
        cands[i].clear()
        eliminatePeers(i, v)
    }

    private fun eliminatePeers(i: Int, v: Int) {
        for (p in PEERS[i]) cands[p].remove(v)
    }

    fun eliminate(i: Int, v: Int) = cands[i].remove(v)
    fun isSolved() = values.none { it == 0 }
}

object SudokuAnalyzer {

    fun analyzeRequiredLevel(puzzle: IntArray): TechniqueLevel {
        val grid = CandidateGrid(puzzle)
        var maxUsed = TechniqueLevel.NAKED_SINGLE

        outer@ while (!grid.isSolved()) {
            for (level in TechniqueLevel.values()) {
                if (level == TechniqueLevel.UNSOLVABLE) break
                if (applyTechnique(grid, level)) {
                    if (level.ordinal > maxUsed.ordinal) maxUsed = level
                    continue@outer
                }
            }
            return TechniqueLevel.UNSOLVABLE
        }
        return maxUsed
    }

    private fun applyTechnique(grid: CandidateGrid, level: TechniqueLevel): Boolean = when (level) {
        TechniqueLevel.NAKED_SINGLE  -> applyNakedSingles(grid)
        TechniqueLevel.HIDDEN_SINGLE -> applyHiddenSingles(grid)
        TechniqueLevel.NAKED_PAIR    -> applyNakedPairs(grid)
        TechniqueLevel.HIDDEN_PAIR   -> applyHiddenPairs(grid)
        TechniqueLevel.POINTING_PAIR -> applyPointingPairs(grid)
        TechniqueLevel.NAKED_TRIPLE  -> applyNakedTriples(grid)
        TechniqueLevel.HIDDEN_TRIPLE -> applyHiddenTriples(grid)
        TechniqueLevel.X_WING        -> applyXWing(grid)
        TechniqueLevel.SWORDFISH     -> applySwordfish(grid)
        TechniqueLevel.UNSOLVABLE    -> false
    }

    private fun applyNakedSingles(grid: CandidateGrid): Boolean {
        var progress = false
        for (i in 0 until 81) {
            if (grid.values[i] == 0 && grid.cands[i].size == 1) {
                grid.place(i, grid.cands[i].first())
                progress = true
            }
        }
        return progress
    }

    private fun applyHiddenSingles(grid: CandidateGrid): Boolean {
        var progress = false
        for (unit in ALL_UNITS) {
            for (v in 1..9) {
                val cells = unit.filter { grid.values[it] == 0 && v in grid.cands[it] }
                if (cells.size == 1) {
                    grid.place(cells[0], v)
                    progress = true
                }
            }
        }
        return progress
    }

    // Two cells in a unit share the exact same 2 candidates → remove those from all other cells in the unit.
    private fun applyNakedPairs(grid: CandidateGrid): Boolean {
        var progress = false
        for (unit in ALL_UNITS) {
            val empty = unit.filter { grid.values[it] == 0 }
            val pairCells = empty.filter { grid.cands[it].size == 2 }
            for (i in pairCells.indices) {
                for (j in i + 1 until pairCells.size) {
                    val a = pairCells[i]; val b = pairCells[j]
                    if (grid.cands[a] == grid.cands[b]) {
                        val pair = grid.cands[a].toSet()
                        for (other in empty) {
                            if (other != a && other != b) {
                                for (v in pair) { if (grid.eliminate(other, v)) progress = true }
                            }
                        }
                    }
                }
            }
        }
        return progress
    }

    // Two values each appear in exactly the same 2 cells in a unit → remove all other candidates from those cells.
    private fun applyHiddenPairs(grid: CandidateGrid): Boolean {
        var progress = false
        for (unit in ALL_UNITS) {
            val empty = unit.filter { grid.values[it] == 0 }
            for (v1 in 1..8) {
                for (v2 in v1 + 1..9) {
                    val c1 = empty.filter { v1 in grid.cands[it] }
                    val c2 = empty.filter { v2 in grid.cands[it] }
                    if (c1.size == 2 && c2.size == 2 && c1.toSet() == c2.toSet()) {
                        val pairValues = setOf(v1, v2)
                        for (c in c1) {
                            for (v in grid.cands[c].toSet()) {
                                if (v !in pairValues && grid.eliminate(c, v)) progress = true
                            }
                        }
                    }
                }
            }
        }
        return progress
    }

    // All candidates for a value inside a box lie in one row or column → remove from rest of that line.
    private fun applyPointingPairs(grid: CandidateGrid): Boolean {
        var progress = false
        for (box in BOXES) {
            val boxSet = box.toHashSet()
            for (v in 1..9) {
                val cells = box.filter { grid.values[it] == 0 && v in grid.cands[it] }
                if (cells.size < 2) continue
                val rows = cells.map { it / 9 }.toSet()
                val cols = cells.map { it % 9 }.toSet()
                if (rows.size == 1) {
                    for (c in ROWS[rows.first()]) { if (c !in boxSet && grid.eliminate(c, v)) progress = true }
                }
                if (cols.size == 1) {
                    for (c in COLS[cols.first()]) { if (c !in boxSet && grid.eliminate(c, v)) progress = true }
                }
            }
        }
        return progress
    }

    // Three cells in a unit whose combined candidates total exactly 3 → remove those from all other cells.
    private fun applyNakedTriples(grid: CandidateGrid): Boolean {
        var progress = false
        for (unit in ALL_UNITS) {
            val empty = unit.filter { grid.values[it] == 0 && grid.cands[it].size in 2..3 }
            for (i in 0 until empty.size - 2) {
                for (j in i + 1 until empty.size - 1) {
                    for (k in j + 1 until empty.size) {
                        val combined = grid.cands[empty[i]] + grid.cands[empty[j]] + grid.cands[empty[k]]
                        if (combined.size == 3) {
                            val triple = setOf(empty[i], empty[j], empty[k])
                            for (c in unit.filter { grid.values[it] == 0 && it !in triple }) {
                                for (v in combined) { if (grid.eliminate(c, v)) progress = true }
                            }
                        }
                    }
                }
            }
        }
        return progress
    }

    // Three values each confined to the same 3 cells in a unit → remove all other candidates from those cells.
    private fun applyHiddenTriples(grid: CandidateGrid): Boolean {
        var progress = false
        for (unit in ALL_UNITS) {
            val empty = unit.filter { grid.values[it] == 0 }
            for (v1 in 1..7) {
                for (v2 in v1 + 1..8) {
                    for (v3 in v2 + 1..9) {
                        val cells = empty.filter {
                            v1 in grid.cands[it] || v2 in grid.cands[it] || v3 in grid.cands[it]
                        }
                        if (cells.size == 3) {
                            val tripleValues = setOf(v1, v2, v3)
                            for (c in cells) {
                                for (v in grid.cands[c].toSet()) {
                                    if (v !in tripleValues && grid.eliminate(c, v)) progress = true
                                }
                            }
                        }
                    }
                }
            }
        }
        return progress
    }

    // Value restricted to the same 2 columns across 2 rows (or 2 rows across 2 columns) → remove from rest of those lines.
    private fun applyXWing(grid: CandidateGrid): Boolean {
        var progress = false
        for (v in 1..9) {
            // Row-based
            val rowPos = (0..8).map { r -> ROWS[r].filter { grid.values[it] == 0 && v in grid.cands[it] } }
            val twoRows = (0..8).filter { rowPos[it].size == 2 }
            for (i in 0 until twoRows.size - 1) {
                for (j in i + 1 until twoRows.size) {
                    val r1 = twoRows[i]; val r2 = twoRows[j]
                    if (rowPos[r1].map { it % 9 }.toSet() == rowPos[r2].map { it % 9 }.toSet()) {
                        for (col in rowPos[r1].map { it % 9 }) {
                            for (cell in COLS[col]) {
                                if (cell / 9 != r1 && cell / 9 != r2 && grid.eliminate(cell, v)) progress = true
                            }
                        }
                    }
                }
            }
            // Column-based
            val colPos = (0..8).map { c -> COLS[c].filter { grid.values[it] == 0 && v in grid.cands[it] } }
            val twoCols = (0..8).filter { colPos[it].size == 2 }
            for (i in 0 until twoCols.size - 1) {
                for (j in i + 1 until twoCols.size) {
                    val c1 = twoCols[i]; val c2 = twoCols[j]
                    if (colPos[c1].map { it / 9 }.toSet() == colPos[c2].map { it / 9 }.toSet()) {
                        for (row in colPos[c1].map { it / 9 }) {
                            for (cell in ROWS[row]) {
                                if (cell % 9 != c1 && cell % 9 != c2 && grid.eliminate(cell, v)) progress = true
                            }
                        }
                    }
                }
            }
        }
        return progress
    }

    // Value restricted to 2-3 columns across 3 rows (all within the same 3 columns) → remove from rest of those columns.
    private fun applySwordfish(grid: CandidateGrid): Boolean {
        var progress = false
        for (v in 1..9) {
            // Row-based
            val rowPos = (0..8).map { r -> ROWS[r].filter { grid.values[it] == 0 && v in grid.cands[it] } }
            val sfRows = (0..8).filter { rowPos[it].size in 2..3 }
            for (i in 0 until sfRows.size - 2) {
                for (j in i + 1 until sfRows.size - 1) {
                    for (k in j + 1 until sfRows.size) {
                        val r1 = sfRows[i]; val r2 = sfRows[j]; val r3 = sfRows[k]
                        val cols = (rowPos[r1] + rowPos[r2] + rowPos[r3]).map { it % 9 }.toSet()
                        if (cols.size == 3) {
                            val rows3 = setOf(r1, r2, r3)
                            for (col in cols) {
                                for (cell in COLS[col]) {
                                    if (cell / 9 !in rows3 && grid.eliminate(cell, v)) progress = true
                                }
                            }
                        }
                    }
                }
            }
            // Column-based
            val colPos = (0..8).map { c -> COLS[c].filter { grid.values[it] == 0 && v in grid.cands[it] } }
            val sfCols = (0..8).filter { colPos[it].size in 2..3 }
            for (i in 0 until sfCols.size - 2) {
                for (j in i + 1 until sfCols.size - 1) {
                    for (k in j + 1 until sfCols.size) {
                        val c1 = sfCols[i]; val c2 = sfCols[j]; val c3 = sfCols[k]
                        val rows = (colPos[c1] + colPos[c2] + colPos[c3]).map { it / 9 }.toSet()
                        if (rows.size == 3) {
                            val cols3 = setOf(c1, c2, c3)
                            for (row in rows) {
                                for (cell in ROWS[row]) {
                                    if (cell % 9 !in cols3 && grid.eliminate(cell, v)) progress = true
                                }
                            }
                        }
                    }
                }
            }
        }
        return progress
    }
}
