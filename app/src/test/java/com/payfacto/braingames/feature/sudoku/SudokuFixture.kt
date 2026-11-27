package com.payfacto.braingames.feature.sudoku

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.sudoku.data.SudokuPuzzleSource
import com.payfacto.braingames.feature.sudoku.model.SudokuBoard
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.testutil.FakeGameSaves
import com.payfacto.braingames.testutil.TestBoards
import com.payfacto.braingames.testutil.fakePreferencesRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

/**
 * One game's worth of dependencies. [newViewModel] can be called again to simulate the
 * ViewModel being recreated with the same SavedStateHandle.
 *
 * The test board has five open cells, 0–4, whose solution is 1–5.
 */
class SudokuFixture(mode: GameMode? = null) {

    val savedState = SavedStateHandle(
        buildMap {
            put(Screen.ARG_DIFFICULTY, Difficulty.EASY.name)
            if (mode != null) put(Screen.ARG_MODE, mode.name)
        }
    )
    val puzzleSource = RecordingSudokuSource()
    private val preferences = fakePreferencesRepository()
    val saves = FakeGameSaves()

    val solution: IntArray = TestBoards.sudoku().solution

    /** Builds a ViewModel on the test scheduler and lets its puzzle load. */
    fun TestScope.newViewModel(): SudokuViewModel =
        SudokuViewModel(savedState, puzzleSource, preferences, testScheduler.timeSource, saves)
            .also { advanceUntilIdle() }

    /** A digit different from the solution at [cell]. */
    fun wrongValueFor(cell: Int): Int = solution[cell] % 9 + 1
}

class RecordingSudokuSource : SudokuPuzzleSource {
    var requests = 0
        private set

    override suspend fun newPuzzle(difficulty: Difficulty): SudokuBoard {
        requests++
        return TestBoards.sudoku()
    }
}

fun SudokuViewModel.write(cell: Int, value: Int) {
    if (state.value.isNotesMode) toggleNotesMode()
    selectCell(cell)
    enterNumber(value)
}

fun SudokuViewModel.note(cell: Int, value: Int) {
    if (!state.value.isNotesMode) toggleNotesMode()
    selectCell(cell)
    enterNumber(value)
    toggleNotesMode()
}

fun SudokuViewModel.erase(cell: Int) {
    selectCell(cell)
    clearCell()
}

/** Writes the correct digit in every open cell. */
fun SudokuViewModel.fillCorrectly(fixture: SudokuFixture) {
    TestBoards.SUDOKU_OPEN_CELLS.forEach { write(it, fixture.solution[it]) }
}
