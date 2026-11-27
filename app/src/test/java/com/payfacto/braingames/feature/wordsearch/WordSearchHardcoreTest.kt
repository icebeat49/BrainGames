package com.payfacto.braingames.feature.wordsearch

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
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

/** Hardcore, like pencil on paper: lines and ticks are free and unchecked; only the typed secret is. */
@OptIn(ExperimentalCoroutinesApi::class)
class WordSearchHardcoreTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val savedState = SavedStateHandle(
        mapOf(Screen.ARG_DIFFICULTY to Difficulty.MEDIUM.name, Screen.ARG_MODE to GameMode.HARDCORE.name)
    )
    private val board: WordSearchBoard = WordSearchBankFiles.load("fr", 12).first().toBoard()
    private val wordA = board.words[0]
    private val wordB = board.words[1]

    private fun TestScope.newViewModel() = WordSearchViewModel(
        savedState,
        object : WordSearchPuzzleSource {
            override suspend fun newPuzzle(difficulty: Difficulty): WordSearchBoard = board
        },
        fakePreferencesRepository(),
        testScheduler.timeSource
    ).also { advanceUntilIdle() }

    @Test
    fun `no hints`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel()

        assertEquals(0, viewModel.session.value.hintsRemaining)
        assertFalse(viewModel.useHint())
    }

    @Test
    fun `any line can be drawn, right or wrong, without ticking any word`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel()
        val notAWord = wordA.cells().drop(1)

        assertEquals(SelectionResult.DRAWN, viewModel.checkSelection(wordA.cells()))
        assertEquals(SelectionResult.DRAWN, viewModel.checkSelection(notAWord))

        val drawn = viewModel.state.value.board
        assertEquals(listOf(wordA.cells(), notAWord), drawn.marks)
        assertTrue("words are ticked by hand only", drawn.words.none { it.found })
    }

    @Test
    fun `tracing a line again, either way round, erases it`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel()
        viewModel.checkSelection(wordA.cells())

        assertEquals(SelectionResult.ERASED, viewModel.checkSelection(wordA.cells().reversed()))
        assertTrue(viewModel.state.value.board.marks.isEmpty())
    }

    @Test
    fun `any word can be ticked off and back on at any time`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel()

        viewModel.onWordTapped(wordB.word)
        assertTrue(viewModel.state.value.board.words.single { it.word == wordB.word }.found)
        assertTrue("ticking draws nothing", viewModel.state.value.board.marks.isEmpty())

        viewModel.onWordTapped(wordB.word)
        assertFalse(viewModel.state.value.board.words.single { it.word == wordB.word }.found)
    }

    @Test
    fun `ticking every word does not win, the secret does`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel()
        board.words.forEach {
            viewModel.checkSelection(it.cells())
            viewModel.onWordTapped(it.word)
        }
        advanceUntilIdle()
        assertTrue(viewModel.state.value.board.allWordsFound())
        assertEquals(board.hiddenWordCells, viewModel.state.value.board.unmarkedCells())
        assertFalse(viewModel.session.value.showWin)

        assertTrue(viewModel.checkSecret(board.hiddenWord))
        advanceUntilIdle()

        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `lines and ticks survive a recreated ViewModel`() = runTest(mainDispatcher.dispatcher) {
        val original = newViewModel()
        original.checkSelection(wordB.cells())
        original.onWordTapped(wordA.word)

        val recreated = newViewModel()

        assertEquals(original.state.value.board, recreated.state.value.board)
        assertEquals(listOf(wordB.cells()), recreated.state.value.board.marks)
        assertTrue(recreated.state.value.board.words.single { it.word == wordA.word }.found)
    }
}
