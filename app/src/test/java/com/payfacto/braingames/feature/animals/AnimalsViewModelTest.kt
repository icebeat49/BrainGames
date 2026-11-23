package com.payfacto.braingames.feature.animals

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.CheckResult
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.ui.grid.GridPoint
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.animals.AnimalsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.animals.data.AnimalsPuzzleSource
import com.payfacto.braingames.feature.animals.engine.AnimalsHint
import com.payfacto.braingames.feature.animals.engine.Technique
import com.payfacto.braingames.feature.animals.model.AnimalsBoard
import com.payfacto.braingames.feature.animals.model.AnimalsPuzzle
import com.payfacto.braingames.feature.animals.model.CellMark
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
class AnimalsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val puzzle = uniqueFiveByFive
    private fun cell(row: Int, col: Int) = puzzle.cellAt(row, col)
    private val emptyBoard = AnimalsBoard.empty(5)

    /** Dependencies for one game; calling newViewModel again simulates recreation. */
    private inner class Fixture(mode: GameMode) {
        val savedState = SavedStateHandle(
            mapOf(Screen.ARG_DIFFICULTY to Difficulty.VERY_EASY.name, Screen.ARG_MODE to mode.name)
        )
        val source = RecordingSource(puzzle)
        private val preferences = fakePreferencesRepository()

        fun TestScope.newViewModel() =
            AnimalsViewModel(savedState, source, preferences, testScheduler.timeSource).also { advanceUntilIdle() }
    }

    private var clock = 10_000L

    private fun AnimalsViewModel.tap(cell: Int, gapMillis: Long = 1_000) {
        val down = clock + gapMillis
        clock = down + 40
        onTap(cell, down, clock, DOUBLE_TAP_TIMEOUT)
    }

    private fun AnimalsViewModel.placeAnimal(cell: Int) {
        tap(cell)
        tap(cell, gapMillis = 80)
    }

    private fun AnimalsViewModel.placeAll(cells: Collection<Int>) = cells.forEach { placeAnimal(it) }

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
        viewModel.placeAnimal(cell(0, 0))

        assertEquals(CellMark.CROSS, viewModel.state.value.board[cell(2, 2)])
        assertEquals(CellMark.ANIMAL, viewModel.state.value.board[cell(0, 0)])
    }

    @Test
    fun `the question toggle changes what a tap writes`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.toggleQuestionMode()
        viewModel.tap(cell(2, 2))

        assertEquals(CellMark.QUESTION, viewModel.state.value.board[cell(2, 2)])
    }

    @Test
    fun `placing an animal never crosses out its neighbours`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeAnimal(cell(2, 2))

        assertEquals(setOf(cell(2, 2)), viewModel.state.value.board.cellsWith(CellMark.ANIMAL))
        assertEquals(emptySet<Int>(), viewModel.state.value.board.cellsWith(CellMark.CROSS))
    }

    // ── Classic ─────────────────────────────────────────────────────────────

    @Test
    fun `classic marks animals that break a rule`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeAnimal(cell(0, 1))
        viewModel.placeAnimal(cell(0, 2))

        assertEquals(setOf(cell(0, 1), cell(0, 2)), viewModel.state.value.conflictCells)
    }

    @Test
    fun `classic conflict marks go away once the conflict is fixed`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeAnimal(cell(0, 1))
        viewModel.placeAnimal(cell(0, 2))

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
        assertEquals(CellMark.ANIMAL, state.board[cell(0, 0)])
        val shown = state.hint!!.hint as AnimalsHint.Deduction
        assertEquals(Technique.SINGLE_CANDIDATE, shown.step.technique)
        assertEquals(setOf(cell(0, 0)), state.hint!!.cells)
        assertEquals(setOf(cell(0, 0)), state.hint!!.areaCells) // region A is one cell
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `a hint first removes a wrong animal`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeAnimal(cell(4, 4))

        viewModel.useHint()

        assertEquals(CellMark.EMPTY, viewModel.state.value.board[cell(4, 4)])
        assertEquals(AnimalsHint.WrongMark(cell(4, 4), CellMark.ANIMAL), viewModel.state.value.hint!!.hint)
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
        viewModel.placeAll(puzzle.solutionCells.filter { viewModel.state.value.board[it] != CellMark.ANIMAL })

        assertTrue(viewModel.session.value.showWin)
    }

    // ── Hardcore ────────────────────────────────────────────────────────────

    @Test
    fun `hardcore shows no conflicts`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()

        viewModel.placeAnimal(cell(0, 1))
        viewModel.placeAnimal(cell(0, 2))

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
    fun `check is enabled only with N animals on the board`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells.take(4))
        val withFour = viewModel.state.value.canCheck

        viewModel.placeAnimal(puzzle.solutionCells.last())

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
    fun `check returns Incorrect with exactly the misplaced animals`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells - cell(0, 0) + cell(4, 4))

        assertEquals(CheckResult.Incorrect(setOf(cell(4, 4))), viewModel.checkSolution())
        assertEquals(setOf(cell(4, 4)), viewModel.state.value.failedCheck?.wrongCells)
    }

    @Test
    fun `show where marks the wrong animals until one of them changes`() = runTest(mainDispatcher.dispatcher) {
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
        viewModel.placeAnimal(cell(2, 2))

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
    fun `clear all empties the board and can be undone`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.tap(cell(1, 1))
        viewModel.placeAnimal(cell(0, 0))
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
            original.placeAnimal(cell(0, 0))
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

    private class RecordingSource(private val puzzle: AnimalsPuzzle) : AnimalsPuzzleSource {
        val requests = mutableListOf<Difficulty>()

        override suspend fun newPuzzle(difficulty: Difficulty): AnimalsPuzzle {
            requests += difficulty
            return puzzle
        }
    }

    private companion object {
        const val DOUBLE_TAP_TIMEOUT = 300L
    }
}
