package com.payfacto.braingames.feature.kakuro.model

data class KakuroBoard(
    val rows: Int,
    val cols: Int,
    val cells: List<List<KakuroCell>>
) {
    fun cellAt(row: Int, col: Int): KakuroCell = cells[row][col]

    fun withInput(row: Int, col: Int, value: Int): KakuroBoard {
        val newCells = cells.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, cell ->
                if (r == row && c == col && cell is KakuroCell.Input) {
                    cell.copy(current = value)
                } else cell
            }
        }
        return copy(cells = newCells)
    }

    fun isComplete(): Boolean {
        // All INPUT cells must be filled
        if (cells.any { row -> row.any { cell -> cell is KakuroCell.Input && (cell as KakuroCell.Input).current == 0 } }) return false
        // All across runs must be valid
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = cells[r][c]
                if (cell is KakuroCell.Clue && cell.acrossSum > 0) {
                    if (!isRunValid(collectAcrossRun(r, c))) return false
                    if (sumRun(collectAcrossRun(r, c)) != cell.acrossSum) return false
                }
                if (cell is KakuroCell.Clue && cell.downSum > 0) {
                    if (!isRunValid(collectDownRun(r, c))) return false
                    if (sumRun(collectDownRun(r, c)) != cell.downSum) return false
                }
            }
        }
        return true
    }

    fun hasConflicts(): Boolean {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = cells[r][c]
                if (cell is KakuroCell.Clue) {
                    if (cell.acrossSum > 0) {
                        val run = collectAcrossRun(r, c)
                        val filled = run.filter { it != 0 }
                        if (filled.size != filled.toSet().size) return true
                        if (run.none { it == 0 } && sumRun(run) != cell.acrossSum) return true
                    }
                    if (cell.downSum > 0) {
                        val run = collectDownRun(r, c)
                        val filled = run.filter { it != 0 }
                        if (filled.size != filled.toSet().size) return true
                        if (run.none { it == 0 } && sumRun(run) != cell.downSum) return true
                    }
                }
            }
        }
        return false
    }

    /**
     * The cells of the across run and the down run that [row], [col] belongs to, the cell itself
     * included. Empty when it is not a cell to fill.
     */
    fun runCellsThrough(row: Int, col: Int): Set<Pair<Int, Int>> {
        if (cellAt(row, col) !is KakuroCell.Input) return emptySet()
        fun isInput(r: Int, c: Int) = r in 0 until rows && c in 0 until cols && cells[r][c] is KakuroCell.Input
        fun walk(dRow: Int, dCol: Int) = generateSequence(row to col) { (r, c) -> (r + dRow) to (c + dCol) }
            .takeWhile { (r, c) -> isInput(r, c) }
        return (walk(0, -1) + walk(0, 1) + walk(-1, 0) + walk(1, 0)).toSet()
    }

    private fun collectAcrossRun(row: Int, startCol: Int): List<Int> {
        val run = mutableListOf<Int>()
        var c = startCol + 1
        while (c < cols && cells[row][c] is KakuroCell.Input) {
            run.add((cells[row][c] as KakuroCell.Input).current)
            c++
        }
        return run
    }

    private fun collectDownRun(startRow: Int, col: Int): List<Int> {
        val run = mutableListOf<Int>()
        var r = startRow + 1
        while (r < rows && cells[r][col] is KakuroCell.Input) {
            run.add((cells[r][col] as KakuroCell.Input).current)
            r++
        }
        return run
    }

    private fun isRunValid(run: List<Int>): Boolean {
        val nonZero = run.filter { it != 0 }
        return nonZero.size == nonZero.toSet().size
    }

    private fun sumRun(run: List<Int>): Int = run.sum()
}
