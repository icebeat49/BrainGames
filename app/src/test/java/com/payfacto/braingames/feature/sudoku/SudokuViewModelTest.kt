package com.payfacto.braingames.feature.sudoku

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.session.SessionState
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.sudoku.data.SudokuPuzzleSource
import com.payfacto.braingames.feature.sudoku.model.SudokuBoard
import com.payfacto.braingames.testutil.FakeGameSaves
import com.payfacto.braingames.testutil.MainDispatcherRule
import com.payfacto.braingames.testutil.TestBoards
import com.payfacto.braingames.testutil.fakePreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SudokuViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val savedState = SavedStateHandle(mapOf(Screen.ARG_DIFFICULTY to Difficulty.HARD.name))
    private val puzzleSource = RecordingSudokuSource()
    private val preferences = fakePreferencesRepository()
    private val saves = FakeGameSaves()

    private fun TestScope.newViewModel() =
        SudokuViewModel(savedState, puzzleSource, preferences, testScheduler.timeSource, saves)

    /** Plays a value, a hint and two notes, with 3.5 s on the clock, then leaves the screen. */
    private fun TestScope.playSomeMoves(viewModel: SudokuViewModel) {
        val solution = TestBoards.sudoku().solution
        viewModel.onScreenVisible()
        viewModel.selectCell(0)
        viewModel.enterNumber(solution[0])
        viewModel.useHint()
        viewModel.selectCell(2)
        viewModel.toggleNotesMode()
        viewModel.enterNumber(4)
        viewModel.enterNumber(7)
        advanceTimeBy(3_500)
        runCurrent()
        viewModel.onScreenHidden()
    }

    @Test
    fun `generates one puzzle at the difficulty from the route`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertEquals(listOf(Difficulty.HARD), puzzleSource.requests)
        assertEquals(TestBoards.sudoku(), viewModel.state.value.board)
        assertFalse(viewModel.state.value.isGenerating)
    }

    @Test
    fun `recreated ViewModel with the same SavedStateHandle does not regenerate`() = runTest(mainDispatcher.dispatcher) {
        playSomeMoves(newViewModel().also { advanceUntilIdle() })

        newViewModel()
        advanceUntilIdle()

        assertEquals(1, puzzleSource.requests.size)
    }

    @Test
    fun `recreated ViewModel restores entries and notes`() = runTest(mainDispatcher.dispatcher) {
        val original = newViewModel().also { advanceUntilIdle() }
        playSomeMoves(original)

        val recreated = newViewModel()
        advanceUntilIdle()

        assertEquals(original.state.value.board, recreated.state.value.board)
        assertEquals(setOf(4, 7), recreated.state.value.board.notes[2])
    }

    @Test
    fun `recreated ViewModel restores hints left and elapsed time`() = runTest(mainDispatcher.dispatcher) {
        playSomeMoves(newViewModel().also { advanceUntilIdle() })

        val recreated = newViewModel()
        advanceUntilIdle()

        assertEquals(
            SessionState(elapsedSeconds = 3L, hintsRemaining = GameSession.DEFAULT_HINTS - 1),
            recreated.session.value
        )
    }

    @Test
    fun `new puzzle generates again and resets the session`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel().also { advanceUntilIdle() }
        playSomeMoves(viewModel)

        viewModel.newPuzzle()
        advanceUntilIdle()

        assertEquals(2, puzzleSource.requests.size)
        assertEquals(SessionState(), viewModel.session.value)
    }

    private class RecordingSudokuSource : SudokuPuzzleSource {
        val requests = mutableListOf<Difficulty>()

        override suspend fun newPuzzle(difficulty: Difficulty): SudokuBoard {
            requests += difficulty
            return TestBoards.sudoku()
        }
    }
}
