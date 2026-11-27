package com.payfacto.braingames.feature.kakuro

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.kakuro.data.KakuroPuzzleSource
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell
import com.payfacto.braingames.testutil.FakeGameSaves
import com.payfacto.braingames.testutil.MainDispatcherRule
import com.payfacto.braingames.testutil.TestBoards
import com.payfacto.braingames.testutil.fakePreferencesRepository
import com.payfacto.braingames.testutil.fakeStatsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KakuroViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val savedState = SavedStateHandle(mapOf(Screen.ARG_DIFFICULTY to Difficulty.EXPERT.name))
    private val puzzleSource = RecordingKakuroSource()
    private val preferences = fakePreferencesRepository()
    private val saves = FakeGameSaves()
    private val stats = fakeStatsStore()

    private fun TestScope.newViewModel() =
        KakuroViewModel(savedState, puzzleSource, preferences, testScheduler.timeSource, saves, stats)

    /** Waits for the puzzle to load, enters 1 in (1,1), then uses a hint, which fills (1,2). */
    private fun TestScope.playSomeMoves(viewModel: KakuroViewModel) {
        advanceUntilIdle()
        viewModel.selectCell(1, 1)
        viewModel.enterNumber(1)
        viewModel.useHint()
    }

    @Test
    fun `loads one puzzle at the difficulty from the route`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertEquals(listOf(Difficulty.EXPERT), puzzleSource.requests)
        assertEquals(TestBoards.kakuro(), viewModel.state.value.board)
    }

    @Test
    fun `recreated ViewModel with the same SavedStateHandle does not reload`() = runTest(mainDispatcher.dispatcher) {
        playSomeMoves(newViewModel())

        newViewModel()
        advanceUntilIdle()

        assertEquals(1, puzzleSource.requests.size)
    }

    @Test
    fun `recreated ViewModel restores entries and hints left`() = runTest(mainDispatcher.dispatcher) {
        val original = newViewModel()
        playSomeMoves(original)

        val recreated = newViewModel()
        advanceUntilIdle()

        assertEquals(original.state.value.board, recreated.state.value.board)
        assertEquals(KakuroCell.Input(solution = 1, current = 1), recreated.state.value.board.cellAt(1, 1))
        assertEquals(GameSession.DEFAULT_HINTS - 1, recreated.session.value.hintsRemaining)
    }

    @Test
    fun `continue after the app was closed restores entries and hints left`() = runTest(mainDispatcher.dispatcher) {
        val original = newViewModel()
        playSomeMoves(original)

        val resumedHandle = SavedStateHandle(
            mapOf(Screen.ARG_DIFFICULTY to Difficulty.EXPERT.name, Screen.ARG_RESUME to true)
        )
        val resumed = KakuroViewModel(resumedHandle, puzzleSource, preferences, testScheduler.timeSource, saves, stats)
        advanceUntilIdle()

        assertEquals(original.state.value.board, resumed.state.value.board)
        assertEquals(GameSession.DEFAULT_HINTS - 1, resumed.session.value.hintsRemaining)
        assertEquals(1, puzzleSource.requests.size)
    }

    private class RecordingKakuroSource : KakuroPuzzleSource {
        val requests = mutableListOf<Difficulty>()

        override suspend fun newPuzzle(difficulty: Difficulty): KakuroBoard {
            requests += difficulty
            return TestBoards.kakuro()
        }
    }
}
