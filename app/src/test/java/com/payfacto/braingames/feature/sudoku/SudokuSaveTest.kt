package com.payfacto.braingames.feature.sudoku

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.session.SessionState
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.testutil.FakeGameSaves
import com.payfacto.braingames.testutil.MainDispatcherRule
import com.payfacto.braingames.testutil.TestBoards
import com.payfacto.braingames.testutil.fakePreferencesRepository
import com.payfacto.braingames.testutil.fakeStatsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** The disk save: a game continued after the app was closed, with a fresh SavedStateHandle. */
@OptIn(ExperimentalCoroutinesApi::class)
class SudokuSaveTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val puzzleSource = RecordingSudokuSource()
    private val preferences = fakePreferencesRepository()
    private val saves = FakeGameSaves()
    private val stats = fakeStatsStore()
    private val solution = TestBoards.sudoku().solution

    /** A game screen opened from the picker: a new handle each time, as after the app was closed. */
    private fun TestScope.openGame(resume: Boolean, mode: GameMode = GameMode.CLASSIC): SudokuViewModel {
        val handle = SavedStateHandle(
            mapOf(
                Screen.ARG_DIFFICULTY to Difficulty.HARD.name,
                Screen.ARG_MODE to mode.name,
                Screen.ARG_RESUME to resume
            )
        )
        return SudokuViewModel(handle, puzzleSource, preferences, testScheduler.timeSource, saves, stats)
            .also { advanceUntilIdle() }
    }

    /** A digit, a hint and a note, with 3.5 s on the clock, then the screen is left. */
    private fun TestScope.playSomeMoves(viewModel: SudokuViewModel) {
        viewModel.onScreenVisible()
        viewModel.selectCell(0)
        viewModel.enterNumber(solution[0])
        viewModel.useHint()
        viewModel.selectCell(2)
        viewModel.toggleNotesMode()
        viewModel.enterNumber(4)
        advanceTimeBy(3_500)
        runCurrent()
        viewModel.onScreenHidden()
    }

    @Test
    fun `a new puzzle is saved as soon as it is ready`() = runTest(mainDispatcher.dispatcher) {
        openGame(resume = false)

        val save = saves[GameType.SUDOKU]
        assertNotNull(save)
        assertEquals(Difficulty.HARD, save!!.difficulty)
        assertEquals(GameMode.CLASSIC, save.mode)
    }

    @Test
    fun `continue brings back the board, undo, hints and time`() = runTest(mainDispatcher.dispatcher) {
        val original = openGame(resume = false)
        playSomeMoves(original)

        val resumed = openGame(resume = true)

        assertEquals(original.state.value.board, resumed.state.value.board)
        assertEquals(true, resumed.state.value.canUndo)
        assertEquals(
            SessionState(elapsedSeconds = 3L, hintsRemaining = GameSession.DEFAULT_HINTS - 1),
            resumed.session.value
        )
        assertEquals(1, puzzleSource.requests)
    }

    @Test
    fun `the clock keeps counting from the saved time`() = runTest(mainDispatcher.dispatcher) {
        playSomeMoves(openGame(resume = false))

        val resumed = openGame(resume = true)
        resumed.onScreenVisible()
        advanceTimeBy(2_000)
        runCurrent()
        resumed.onScreenHidden()

        assertEquals(5L, resumed.session.value.elapsedSeconds)
    }

    @Test
    fun `continue without a save starts a new puzzle`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = openGame(resume = true)

        assertEquals(TestBoards.sudoku(), viewModel.state.value.board)
        assertEquals(1, puzzleSource.requests)
    }

    @Test
    fun `a save from another mode is not continued`() = runTest(mainDispatcher.dispatcher) {
        playSomeMoves(openGame(resume = false, mode = GameMode.HARDCORE))

        val viewModel = openGame(resume = true, mode = GameMode.CLASSIC)

        assertEquals(TestBoards.sudoku(), viewModel.state.value.board)
        assertEquals(GameMode.CLASSIC, saves[GameType.SUDOKU]?.mode)
    }

    @Test
    fun `starting a new game replaces the save`() = runTest(mainDispatcher.dispatcher) {
        playSomeMoves(openGame(resume = false))

        openGame(resume = false)

        val resumed = openGame(resume = true)
        assertEquals(TestBoards.sudoku(), resumed.state.value.board)
    }

    @Test
    fun `winning deletes the save`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = openGame(resume = false)

        OPEN_CELLS.forEach { cell ->
            viewModel.selectCell(cell)
            viewModel.enterNumber(solution[cell])
        }
        viewModel.onScreenHidden()

        assertNull(saves[GameType.SUDOKU])
    }

    @Test
    fun `a Hardcore win by check deletes the save`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = openGame(resume = false, mode = GameMode.HARDCORE)
        OPEN_CELLS.forEach { cell ->
            viewModel.selectCell(cell)
            viewModel.enterNumber(solution[cell])
        }

        viewModel.checkSolution()

        assertNull(saves[GameType.SUDOKU])
    }

    private companion object {
        /** The test board's empty cells. */
        val OPEN_CELLS = 0..4
    }
}
