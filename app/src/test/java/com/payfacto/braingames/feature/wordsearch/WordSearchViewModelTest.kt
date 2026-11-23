package com.payfacto.braingames.feature.wordsearch

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.wordsearch.data.FallbackPuzzles
import com.payfacto.braingames.feature.wordsearch.data.WordSearchPuzzleSource
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard
import com.payfacto.braingames.testutil.MainDispatcherRule
import com.payfacto.braingames.testutil.fakePreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WordSearchViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val savedState = SavedStateHandle(mapOf(Screen.ARG_DIFFICULTY to Difficulty.VERY_EASY.name))
    private val puzzleSource = RecordingWordSearchSource()
    private val preferences = fakePreferencesRepository()

    /** "SNOW" sits on the bottom row of the 8×8 fallback board, columns 4–7. */
    private val snowCells = (4..7).map { col -> Pair(7, col) }

    private fun TestScope.newViewModel() =
        WordSearchViewModel(savedState, puzzleSource, preferences, testScheduler.timeSource)

    @Test
    fun `generates one puzzle at the difficulty from the route`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertEquals(listOf(Difficulty.VERY_EASY), puzzleSource.requests)
        assertEquals(FallbackPuzzles.enSize8, viewModel.state.value.board)
    }

    @Test
    fun `recreated ViewModel with the same SavedStateHandle does not regenerate`() = runTest(mainDispatcher.dispatcher) {
        newViewModel().also { advanceUntilIdle() }.checkSelection(snowCells)

        newViewModel()
        advanceUntilIdle()

        assertEquals(1, puzzleSource.requests.size)
    }

    @Test
    fun `recreated ViewModel restores the board and found words`() = runTest(mainDispatcher.dispatcher) {
        val original = newViewModel().also { advanceUntilIdle() }
        original.checkSelection(snowCells)

        val recreated = newViewModel()
        advanceUntilIdle()

        val restored = recreated.state.value.board
        assertEquals(original.state.value.board, restored)
        assertEquals(original.state.value.board.hiddenWordCells, restored.hiddenWordCells)
        assertTrue(restored.words.single { it.word == "SNOW" }.found)
    }

    private class RecordingWordSearchSource : WordSearchPuzzleSource {
        val requests = mutableListOf<Difficulty>()

        override suspend fun newPuzzle(difficulty: Difficulty): WordSearchBoard {
            requests += difficulty
            return FallbackPuzzles.enSize8
        }
    }
}
