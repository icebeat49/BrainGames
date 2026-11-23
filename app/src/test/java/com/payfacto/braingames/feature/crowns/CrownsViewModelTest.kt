package com.payfacto.braingames.feature.crowns

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.CheckResult
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.ui.grid.GridPoint
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.crowns.CrownsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.crowns.data.CrownsPuzzleSource
import com.payfacto.braingames.feature.crowns.engine.CrownsHint
import com.payfacto.braingames.feature.crowns.engine.Technique
import com.payfacto.braingames.feature.crowns.input.TapEvent
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import com.payfacto.braingames.feature.crowns.model.CellMark
import com.payfacto.braingames.testutil.MainDispatcherRule
import com.payfacto.braingames.testutil.fakePreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrownsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val puzzle = uniqueFiveByFive
    private fun cell(row: Int, col: Int) = puzzle.cellAt(row, col)
    private val emptyBoard = CrownsBoard.empty(5)

    /** Dependencies for one game; calling newViewModel again simulates recreation. */
    private inner class Fixture(mode: GameMode) {
        val savedState = SavedStateHandle(
            mapOf(Screen.ARG_DIFFICULTY to Difficulty.VERY_EASY.name, Screen.ARG_MODE to mode.name)
        )
        val source = RecordingSource(puzzle)
        private val preferences = fakePreferencesRepository()

        fun TestScope.newViewModel() =
            CrownsViewModel(savedState, source, preferences, testScheduler.timeSource).also { advanceUntilIdle() }
    }

    private var clock = 10_000L

    private fun CrownsViewModel.tap(cell: Int, gapMillis: Long = 1_000) {
        val down = clock + gapMillis
        clock = down + 40
        val point = GridPoint(cell / puzzle.size + 0.5f, cell % puzzle.size + 0.5f)
        onTap(TapEvent(point, down, clock, DOUBLE_TAP_TIMEOUT, snapRadiusCells = 0.2f))
    }

    private fun CrownsViewModel.placeCrown(cell: Int) {
        tap(cell)
        tap(cell, gapMillis = 80)
    }

    private fun CrownsViewModel.placeAll(cells: Collection<Int>) = cells.forEach { placeCrown(it) }

    private fun TestScope.classic() = with(Fixture(GameMode.CLASSIC)) { newViewModel() }
    private fun TestScope.hardcore() = with(Fixture(GameMode.HARDCORE)) { newViewModel() }

    // ── Setup ───────────────────────────────────────────────────────────────

    @Test
    fun `reads difficulty and mode from the route and loads one puzzle`() = runTest(mainDispatcher.dispatcher) {
        val fixture = Fixture(GameMode.HARDCORE)
        val viewModel = with(fixture) { newViewModel() }

        assertEquals(Difficulty.VERY_EASY, viewModel.state.value.difficulty)
        assertEquals(GameMode.HARDCORE, viewModel.state.value.mode)
        assertEquals(listOf(Difficulty.VERY_EASY), fixture.source.requests)
        assertEquals(emptyBoard, viewModel.state.value.board)
    }

    @Test
    fun `taps reach the board through the gesture rules`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.tap(cell(2, 2))
        viewModel.placeCrown(cell(0, 0))

        assertEquals(CellMark.CROSS, viewModel.state.value.board[cell(2, 2)])
        assertEquals(CellMark.CROWN, viewModel.state.value.board[cell(0, 0)])
    }

    @Test
    fun `the question toggle changes what a tap writes`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.toggleQuestionMode()
        viewModel.tap(cell(2, 2))

        assertEquals(CellMark.QUESTION, viewModel.state.value.board[cell(2, 2)])
    }

    @Test
    fun `placing a crown never crosses out its neighbours`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeCrown(cell(2, 2))

        assertEquals(setOf(cell(2, 2)), viewModel.state.value.board.cellsWith(CellMark.CROWN))
        assertEquals(emptySet<Int>(), viewModel.state.value.board.cellsWith(CellMark.CROSS))
    }

    // ── Classic ─────────────────────────────────────────────────────────────

    @Test
    fun `classic marks crowns that break a rule`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeCrown(cell(0, 1))
        viewModel.placeCrown(cell(0, 2))

        assertEquals(setOf(cell(0, 1), cell(0, 2)), viewModel.state.value.conflictCells)
    }

    @Test
    fun `classic conflict marks go away once the conflict is fixed`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeCrown(cell(0, 1))
        viewModel.placeCrown(cell(0, 2))

        viewModel.tap(cell(0, 2))

        assertEquals(emptySet<Int>(), viewModel.state.value.conflictCells)
    }

    @Test
    fun `classic wins automatically when the grid is solved`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeAll(puzzle.solutionCells)

        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `a classic hint is applied, shown and spends one hint`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        val used = viewModel.useHint()

        val state = viewModel.state.value
        assertTrue(used)
        assertEquals(CellMark.CROWN, state.board[cell(0, 0)])
        val shown = state.hint!!.hint as CrownsHint.Deduction
        assertEquals(Technique.SINGLE_CANDIDATE, shown.step.technique)
        assertEquals(setOf(cell(0, 0)), state.hint!!.cells)
        assertEquals(setOf(cell(0, 0)), state.hint!!.areaCells) // region A is one cell
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `a hint first removes a wrong crown`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeCrown(cell(4, 4))

        viewModel.useHint()

        assertEquals(CellMark.EMPTY, viewModel.state.value.board[cell(4, 4)])
        assertEquals(CrownsHint.WrongMark(cell(4, 4), CellMark.CROWN), viewModel.state.value.hint!!.hint)
    }

    @Test
    fun `a hint is one undo step and the hint stays spent`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.useHint()

        viewModel.undo()

        assertEquals(emptyBoard, viewModel.state.value.board)
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `the hint highlight goes away with the next move`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.useHint()

        viewModel.tap(cell(4, 4))

        assertNull(viewModel.state.value.hint)
    }

    @Test
    fun `following hints alone solves the puzzle and wins`() = runTest(mainDispatcher.dispatcher) {
        val fixture = Fixture(GameMode.CLASSIC)
        val viewModel = with(fixture) { newViewModel() }

        repeat(GameSession.DEFAULT_HINTS) { viewModel.useHint() }
        // Five hints may not finish a 5x5; place what is left from the solution.
        viewModel.placeAll(puzzle.solutionCells.filter { viewModel.state.value.board[it] != CellMark.CROWN })

        assertTrue(viewModel.session.value.showWin)
    }

    // ── Hardcore ────────────────────────────────────────────────────────────

    @Test
    fun `hardcore shows no conflicts`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()

        viewModel.placeCrown(cell(0, 1))
        viewModel.placeCrown(cell(0, 2))

        assertEquals(emptySet<Int>(), viewModel.state.value.conflictCells)
    }

    @Test
    fun `hardcore has no hints`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()

        assertFalse(viewModel.useHint())
        assertEquals(0, viewModel.session.value.hintsRemaining)
        assertEquals(emptyBoard, viewModel.state.value.board)
    }

    @Test
    fun `hardcore does not win automatically`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()

        viewModel.placeAll(puzzle.solutionCells)

        assertFalse(viewModel.session.value.showWin)
    }

    @Test
    fun `check is enabled only with N crowns on the board`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells.take(4))
        val withFour = viewModel.state.value.canCheck

        viewModel.placeCrown(puzzle.solutionCells.last())

        assertFalse(withFour)
        assertTrue(viewModel.state.value.canCheck)
    }

    @Test
    fun `check on the solution returns Solved and shows the win`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells)

        assertEquals(CheckResult.Solved, viewModel.checkSolution())
        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `check returns Incorrect with exactly the misplaced crowns`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells - cell(0, 0) + cell(4, 4))

        assertEquals(CheckResult.Incorrect(setOf(cell(4, 4))), viewModel.checkSolution())
        assertEquals(setOf(cell(4, 4)), viewModel.state.value.failedCheck?.wrongCells)
    }

    @Test
    fun `show where marks the wrong crowns until one of them changes`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells - cell(0, 0) + cell(4, 4))
        viewModel.checkSolution()
        viewModel.showWrongCells()

        viewModel.tap(cell(2, 0))  // an unrelated cell: marks stay
        val afterOtherCell = viewModel.state.value.errorCells
        viewModel.tap(cell(4, 4))  // the marked cell: marks go
        val afterMarkedCell = viewModel.state.value.errorCells

        assertEquals(setOf(cell(4, 4)), afterOtherCell)
        assertEquals(emptySet<Int>(), afterMarkedCell)
    }

    @Test
    fun `show how many keeps the dialog open with the count`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells - cell(0, 0) + cell(4, 4))
        viewModel.checkSolution()

        viewModel.showWrongCount()

        assertEquals(1, viewModel.state.value.failedCheck?.wrongCount)
        assertTrue(viewModel.state.value.failedCheck!!.isCountShown)
    }

    // ── Undo ────────────────────────────────────────────────────────────────

    @Test
    fun `a double tap is one undo step`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeCrown(cell(2, 2))

        viewModel.undo()

        assertEquals(emptyBoard, viewModel.state.value.board)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `a drag is one undo step`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.onDragStart(GridPoint(4.5f, 0.5f))
        viewModel.onDragMove(GridPoint(4.5f, 2.5f))
        viewModel.onDragMove(GridPoint(4.5f, 4.5f))
        viewModel.onDragEnd()
        val crossed = viewModel.state.value.board.cellsWith(CellMark.CROSS)

        viewModel.undo()

        assertEquals((20..24).toSet(), crossed)
        assertEquals(emptyBoard, viewModel.state.value.board)
    }

    @Test
    fun `an erase drag clears marks, keeps the crown and is one undo step`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        (20..23).forEach { viewModel.tap(it) }         // X on (4,0)..(4,3)
        viewModel.placeCrown(cell(4, 4))
        val beforeErase = viewModel.state.value.board

        viewModel.onDragStart(GridPoint(4.5f, 0.5f))
        viewModel.onDragMove(GridPoint(4.5f, 4.5f))
        viewModel.onDragEnd()
        val erased = viewModel.state.value.board
        viewModel.undo()

        assertEquals(emptySet<Int>(), erased.cellsWith(CellMark.CROSS))
        assertEquals(CellMark.CROWN, erased[cell(4, 4)])
        assertEquals(beforeErase, viewModel.state.value.board)
    }

    @Test
    fun `clear marks removes every cross and question mark and keeps the crowns`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.tap(cell(1, 1))
        viewModel.toggleQuestionMode()
        viewModel.tap(cell(3, 3))
        viewModel.placeCrown(cell(0, 0))

        viewModel.clearMarks()

        val board = viewModel.state.value.board
        assertEquals(emptyBoard.withMark(cell(0, 0), CellMark.CROWN), board)
        assertFalse(viewModel.state.value.hasMarks)
    }

    @Test
    fun `clear marks is one undo step`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.tap(cell(1, 1))
        viewModel.tap(cell(2, 3))
        val beforeClear = viewModel.state.value.board

        viewModel.clearMarks()
        viewModel.undo()

        assertEquals(beforeClear, viewModel.state.value.board)
    }

    @Test
    fun `clear marks is unavailable without marks`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        val onEmptyBoard = viewModel.state.value.hasMarks

        viewModel.placeCrown(cell(0, 0))

        assertFalse(onEmptyBoard)
        assertFalse(viewModel.state.value.hasMarks)
    }

    @Test
    fun `clear marks with no marks changes nothing and adds no undo`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.clearMarks()

        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `clear all empties the board and can be undone`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.tap(cell(1, 1))
        viewModel.placeCrown(cell(0, 0))
        val beforeClear = viewModel.state.value.board

        viewModel.clearAll()
        val cleared = viewModel.state.value.board
        viewModel.undo()

        assertEquals(emptyBoard, cleared)
        assertEquals(beforeClear, viewModel.state.value.board)
    }

    @Test
    fun `undo history is empty after a new puzzle`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.tap(cell(1, 1))

        viewModel.newPuzzle()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canUndo)
        assertEquals(emptyBoard, viewModel.state.value.board)
    }

    // ── Saved state ─────────────────────────────────────────────────────────

    @Test
    fun `a recreated ViewModel restores puzzle, marks, undo, toggle and mode without regenerating`() =
        runTest(mainDispatcher.dispatcher) {
            val fixture = Fixture(GameMode.HARDCORE)
            val original = with(fixture) { newViewModel() }
            original.tap(cell(1, 1))
            original.placeCrown(cell(0, 0))
            original.toggleQuestionMode()

            val recreated = with(fixture) { newViewModel() }
            val restored = recreated.state.value
            recreated.undo()
            recreated.undo()

            assertEquals(1, fixture.source.requests.size)
            assertEquals(puzzle, restored.puzzle)
            assertEquals(original.state.value.board, restored.board)
            assertTrue(restored.isQuestionMode)
            assertEquals(GameMode.HARDCORE, restored.mode)
            assertEquals(emptyBoard, recreated.state.value.board)
        }

    private class RecordingSource(private val puzzle: CrownsPuzzle) : CrownsPuzzleSource {
        val requests = mutableListOf<Difficulty>()

        override suspend fun newPuzzle(difficulty: Difficulty): CrownsPuzzle {
            requests += difficulty
            return puzzle
        }
    }

    private companion object {
        const val DOUBLE_TAP_TIMEOUT = 300L
    }
}
