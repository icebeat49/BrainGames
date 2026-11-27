package com.payfacto.braingames.feature.wordsearch

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.wordsearch.data.WordSearchPuzzleSource
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard
import com.payfacto.braingames.testutil.MainDispatcherRule
import com.payfacto.braingames.testutil.fakePreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WordSearchViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val savedState = SavedStateHandle(mapOf(Screen.ARG_DIFFICULTY to Difficulty.MEDIUM.name))
    private val board = WordSearchBankFiles.load("fr", 12).first().toBoard()
    private val puzzleSource = RecordingWordSearchSource(board)
    private val preferences = fakePreferencesRepository()
    private val firstWord = board.words.first()

    private fun TestScope.newViewModel() =
        WordSearchViewModel(savedState, puzzleSource, preferences, testScheduler.timeSource)

    @Test
    fun `serves one puzzle at the difficulty from the route`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertEquals(listOf(Difficulty.MEDIUM), puzzleSource.requests)
        assertEquals(board, viewModel.state.value.board)
    }

    @Test
    fun `a correct selection crosses the word off, in either direction`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel().also { advanceUntilIdle() }

        assertTrue(viewModel.checkSelection(firstWord.cells().reversed()))
        assertTrue(viewModel.state.value.board.words.first().found)
        assertFalse("already found", viewModel.checkSelection(firstWord.cells()))
    }

    @Test
    fun `a selection that is not a listed word is not kept`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel().also { advanceUntilIdle() }

        assertFalse(viewModel.checkSelection(firstWord.cells().drop(1)))
        assertTrue(viewModel.state.value.board.words.none { it.found })
    }

    @Test
    fun `finding every word does not win, typing the secret does`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel().also { advanceUntilIdle() }
        board.words.forEach { viewModel.checkSelection(it.cells()) }
        advanceUntilIdle()
        assertTrue(viewModel.state.value.board.allWordsFound())
        assertFalse(viewModel.session.value.showWin)

        assertFalse(viewModel.checkSecret("pas ça"))
        assertTrue(viewModel.checkSecret(board.hiddenWord.lowercase()))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.board.secretFound)
        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `the secret is accepted with or without spaces and accents`() {
        val phrase = board.copy(hiddenWord = "BIEN JOUE")

        assertTrue(phrase.isSecret("bien joué"))
        assertTrue(phrase.isSecret("BIENJOUE"))
        assertTrue(phrase.isSecret(" Bien-joué ! "))
        assertFalse(phrase.isSecret("BIEN"))
    }

    @Test
    fun `recreated ViewModel restores the board, found words and secret`() = runTest(mainDispatcher.dispatcher) {
        val original = newViewModel().also { advanceUntilIdle() }
        original.checkSelection(firstWord.cells())
        original.checkSecret(board.hiddenWord)

        val recreated = newViewModel()
        advanceUntilIdle()

        assertEquals(1, puzzleSource.requests.size)
        val restored = recreated.state.value.board
        assertEquals(original.state.value.board, restored)
        assertEquals(board.hiddenWordCells, restored.hiddenWordCells)
        assertTrue(restored.words.first().found)
        assertTrue(restored.secretFound)
    }

    private class RecordingWordSearchSource(private val board: WordSearchBoard) : WordSearchPuzzleSource {
        val requests = mutableListOf<Difficulty>()

        override suspend fun newPuzzle(difficulty: Difficulty): WordSearchBoard {
            requests += difficulty
            return board
        }
    }
}
