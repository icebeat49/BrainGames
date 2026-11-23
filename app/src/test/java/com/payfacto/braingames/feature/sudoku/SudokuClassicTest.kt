package com.payfacto.braingames.feature.sudoku

import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.feature.sudoku.model.SudokuAssists
import com.payfacto.braingames.feature.sudoku.model.SudokuMode
import com.payfacto.braingames.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Classic must behave exactly as before Hardcore existed (plus Undo). */
@OptIn(ExperimentalCoroutinesApi::class)
class SudokuClassicTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    /** No mode in the route: the old `sudoku/{difficulty}` links. */
    private val fixture = SudokuFixture(mode = null)

    @Test
    fun `a route without a mode starts Classic`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }

        assertEquals(SudokuMode.CLASSIC, viewModel.state.value.mode)
    }

    @Test
    fun `classic keeps every assist`() {
        assertEquals(
            SudokuAssists(
                highlightPeers = true,
                highlightSameNumber = true,
                showConflicts = true,
                maxHints = GameSession.DEFAULT_HINTS,
                autoDetectWin = true,
                clearCellNotesOnEntry = true
            ),
            SudokuMode.CLASSIC.assists
        )
    }

    @Test
    fun `a hint fills the first wrong cell and spends one hint`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }

        val used = viewModel.useHint()

        assertTrue(used)
        assertEquals(fixture.solution[0], viewModel.state.value.board.current[0])
        assertEquals(0, viewModel.state.value.selectedCell)
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `hints stop when none are left`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.write(cell = 0, value = fixture.wrongValueFor(0))
        repeat(GameSession.DEFAULT_HINTS) { viewModel.useHint() }

        val used = viewModel.useHint()

        assertFalse(used)
    }

    @Test
    fun `completing the grid wins automatically`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }

        viewModel.fillCorrectly(fixture)

        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `a hint that completes the grid wins automatically`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        (1..4).forEach { viewModel.write(it, fixture.solution[it]) }

        viewModel.useHint()

        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `a duplicate digit is reported as a conflict`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        // Cell 1 sits in row 0, which already holds the given 9 at cell 8.
        viewModel.write(cell = 1, value = 9)

        assertTrue(viewModel.state.value.board.conflictsAt(1))
    }

    @Test
    fun `there is no Check button in Classic`() {
        assertTrue(SudokuMode.CLASSIC.assists.autoDetectWin)
    }
}
