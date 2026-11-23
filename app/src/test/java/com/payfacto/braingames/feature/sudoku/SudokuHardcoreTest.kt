package com.payfacto.braingames.feature.sudoku

import com.payfacto.braingames.core.model.CheckResult
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.testutil.MainDispatcherRule
import com.payfacto.braingames.testutil.TestBoards
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SudokuHardcoreTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val fixture = SudokuFixture(GameMode.HARDCORE)

    /** Fills every open cell, with wrong digits in [wrongCells]. */
    private fun SudokuViewModel.fillWithMistakes(vararg wrongCells: Int) {
        TestBoards.SUDOKU_OPEN_CELLS.forEach { cell ->
            write(cell, if (cell in wrongCells) fixture.wrongValueFor(cell) else fixture.solution[cell])
        }
    }

    // ── No help ─────────────────────────────────────────────────────────────

    @Test
    fun `reads the mode from the route`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }

        assertEquals(GameMode.HARDCORE, viewModel.state.value.mode)
    }

    @Test
    fun `starts with no hints`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }

        assertEquals(0, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `useHint does nothing`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        val before = viewModel.state.value.board

        val used = viewModel.useHint()

        assertFalse(used)
        assertEquals(before, viewModel.state.value.board)
    }

    @Test
    fun `completing the grid correctly does not trigger the win`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }

        viewModel.fillCorrectly(fixture)

        assertTrue(viewModel.state.value.board.isComplete())
        assertFalse(viewModel.session.value.showWin)
    }

    @Test
    fun `writing a digit hides only that cell's notes`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.note(cell = 0, value = 7)
        viewModel.note(cell = 1, value = 8)

        viewModel.write(cell = 0, value = 1)

        assertEquals(emptySet<Int>(), viewModel.state.value.board.notes[0])
        assertEquals(setOf(8), viewModel.state.value.board.notes[1])
    }

    // ── Check ───────────────────────────────────────────────────────────────

    @Test
    fun `check is unavailable until every cell has a value`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.write(cell = 0, value = 1)

        assertFalse(viewModel.state.value.canCheck)
    }

    @Test
    fun `check is available once every cell has a value, right or wrong`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }

        viewModel.fillWithMistakes(2)

        assertTrue(viewModel.state.value.canCheck)
    }

    @Test
    fun `checkSolution returns Solved and shows the win for a correct grid`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.fillCorrectly(fixture)

        val result = viewModel.checkSolution()

        assertEquals(CheckResult.Solved, result)
        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `checkSolution returns Incorrect with exactly the wrong cells`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.fillWithMistakes(1, 3)

        val result = viewModel.checkSolution()

        assertEquals(CheckResult.Incorrect(setOf(1, 3)), result)
    }

    @Test
    fun `a failed check opens the dialog and costs nothing`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.fillWithMistakes(4)
        val sessionBefore = viewModel.session.value
        val boardBefore = viewModel.state.value.board

        viewModel.checkSolution()

        assertEquals(setOf(4), viewModel.state.value.failedCheck?.wrongCells)
        assertEquals(sessionBefore, viewModel.session.value)
        assertEquals(boardBefore, viewModel.state.value.board)
    }

    @Test
    fun `keep going closes the dialog without marking cells`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.fillWithMistakes(4)
        viewModel.checkSolution()

        viewModel.dismissFailedCheck()

        assertNull(viewModel.state.value.failedCheck)
        assertEquals(emptySet<Int>(), viewModel.state.value.errorCells)
    }

    @Test
    fun `show how many keeps the dialog open with the count`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.fillWithMistakes(0, 2, 4)
        viewModel.checkSolution()

        viewModel.showWrongCount()

        val failedCheck = viewModel.state.value.failedCheck
        assertTrue(failedCheck!!.isCountShown)
        assertEquals(3, failedCheck.wrongCount)
    }

    @Test
    fun `show where closes the dialog and marks the wrong cells`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.fillWithMistakes(1, 3)
        viewModel.checkSolution()

        viewModel.showWrongCells()

        assertNull(viewModel.state.value.failedCheck)
        assertEquals(setOf(1, 3), viewModel.state.value.errorCells)
    }

    @Test
    fun `changing any cell clears the revealed marks`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.fillWithMistakes(1, 3)
        viewModel.checkSolution()
        viewModel.showWrongCells()

        viewModel.note(cell = 0, value = 9)

        assertEquals(emptySet<Int>(), viewModel.state.value.errorCells)
    }

    @Test
    fun `undo also clears the revealed marks`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.fillWithMistakes(1, 3)
        viewModel.checkSolution()
        viewModel.showWrongCells()

        viewModel.undo()

        assertEquals(emptySet<Int>(), viewModel.state.value.errorCells)
    }

    // ── Saved state ─────────────────────────────────────────────────────────

    @Test
    fun `mode survives ViewModel recreation through SavedStateHandle`() = runTest(mainDispatcher.dispatcher) {
        with(fixture) { newViewModel() }.write(cell = 0, value = 1)

        val recreated = with(fixture) { newViewModel() }

        assertEquals(GameMode.HARDCORE, recreated.state.value.mode)
        assertFalse(recreated.useHint())
        assertEquals(1, fixture.puzzleSource.requests)
    }
}
