package com.payfacto.braingames.feature.sudoku

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.stats.GameStats
import com.payfacto.braingames.core.stats.StatsKey
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.testutil.FakeGameSaves
import com.payfacto.braingames.testutil.MainDispatcherRule
import com.payfacto.braingames.testutil.TestBoards
import com.payfacto.braingames.testutil.fakePreferencesRepository
import com.payfacto.braingames.testutil.fakeStatsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** When a Sudoku counts as played and won. Kakuro and Crowns use the same recorder. */
@OptIn(ExperimentalCoroutinesApi::class)
class SudokuStatsTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val puzzleSource = RecordingSudokuSource()
    private val preferences = fakePreferencesRepository()
    private val saves = FakeGameSaves()
    private val stats = fakeStatsStore()
    private val solution = TestBoards.sudoku().solution

    private fun TestScope.openGame(mode: GameMode = GameMode.CLASSIC, resume: Boolean = false): SudokuViewModel {
        val handle = SavedStateHandle(
            mapOf(
                Screen.ARG_DIFFICULTY to Difficulty.MEDIUM.name,
                Screen.ARG_MODE to mode.name,
                Screen.ARG_RESUME to resume
            )
        )
        return SudokuViewModel(handle, puzzleSource, preferences, testScheduler.timeSource, saves, stats)
            .also { advanceUntilIdle() }
    }

    private suspend fun statsOf(mode: GameMode = GameMode.CLASSIC): GameStats =
        stats.all.first()[StatsKey(GameType.SUDOKU, Difficulty.MEDIUM, mode)] ?: GameStats()

    private fun SudokuViewModel.play(cell: Int, value: Int = solution[cell]) {
        selectCell(cell)
        enterNumber(value)
    }

    /** Fills the test board's five empty cells with 10 s on the clock. */
    private fun TestScope.solve(viewModel: SudokuViewModel) {
        viewModel.onScreenVisible()
        advanceTimeBy(10_000)
        runCurrent()
        (0..4).forEach { viewModel.play(it) }
        viewModel.onScreenHidden()
    }

    @Test
    fun `opening a puzzle without playing it does not count`() = runTest(mainDispatcher.dispatcher) {
        openGame()

        assertEquals(GameStats(), statsOf())
    }

    @Test
    fun `the first move counts the game as played, once`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = openGame()

        viewModel.play(0)
        viewModel.play(1)
        viewModel.play(2)

        assertEquals(GameStats(played = 1), statsOf())
    }

    @Test
    fun `a win counts once, with its time`() = runTest(mainDispatcher.dispatcher) {
        solve(openGame())

        assertEquals(GameStats(played = 1, won = 1, bestMs = 10_000, totalWonMs = 10_000), statsOf())
    }

    @Test
    fun `continuing a saved game does not count it again`() = runTest(mainDispatcher.dispatcher) {
        openGame().play(0)

        openGame(resume = true).play(1)

        assertEquals(1, statsOf().played)
    }

    @Test
    fun `each new puzzle played counts`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = openGame()
        viewModel.play(0)

        viewModel.newPuzzle()
        advanceUntilIdle()
        viewModel.play(0)

        assertEquals(2, statsOf().played)
    }

    @Test
    fun `Hardcore counts apart, and checking twice wins once`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = openGame(GameMode.HARDCORE)
        (0..4).forEach { viewModel.play(it) }

        viewModel.checkSolution()
        viewModel.checkSolution()

        assertEquals(1, statsOf(GameMode.HARDCORE).won)
        assertEquals(GameStats(), statsOf(GameMode.CLASSIC))
    }
}
