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

@OptIn(ExperimentalCoroutinesApi::class)
class WordSearchUndoTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val board: WordSearchBoard = WordSearchBankFiles.load("fr", 12).first().toBoard()
    private val wordA = board.words[0]
    private val wordB = board.words[1]

    private fun TestScope.newViewModel(mode: GameMode, savedState: SavedStateHandle = stateFor(mode)) =
        WordSearchViewModel(
            savedState,
            object : WordSearchPuzzleSource {
                override suspend fun newPuzzle(difficulty: Difficulty): WordSearchBoard = board
            },
            fakePreferencesRepository(),
            testScheduler.timeSource
        ).also { advanceUntilIdle() }

    private fun stateFor(mode: GameMode) =
        SavedStateHandle(mapOf(Screen.ARG_DIFFICULTY to Difficulty.MEDIUM.name, Screen.ARG_MODE to mode.name))

    @Test
    fun `nothing to undo on a new puzzle`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel(GameMode.CLASSIC)

        assertFalse(viewModel.state.value.canUndo)
        assertFalse(viewModel.undo())
    }

    @Test
    fun `Classic - undo puts the last found word back in play and removes its line`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel(GameMode.CLASSIC)
        viewModel.checkSelection(wordA.cells())
        viewModel.checkSelection(wordB.cells())

        assertTrue(viewModel.undo())

        val undone = viewModel.state.value.board
        assertTrue(undone.words.single { it.word == wordA.word }.found)
        assertFalse(undone.words.single { it.word == wordB.word }.found)
        assertEquals(listOf(wordA.cells()), undone.marks)
        assertTrue(viewModel.state.value.canUndo)
    }

    @Test
    fun `Hardcore - undo takes back lines, erasures and ticks, newest first`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel(GameMode.HARDCORE)
        viewModel.checkSelection(wordA.cells())   // draw
        viewModel.onWordTapped(wordB.word)         // tick
        viewModel.checkSelection(wordA.cells())   // erase

        viewModel.undo()
        assertEquals("erasure undone", listOf(wordA.cells()), viewModel.state.value.board.marks)
        viewModel.undo()
        assertFalse("tick undone", viewModel.state.value.board.words.single { it.word == wordB.word }.found)
        viewModel.undo()
        assertTrue("line undone", viewModel.state.value.board.marks.isEmpty())

        assertEquals(board, viewModel.state.value.board)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `undo history survives a recreated ViewModel`() = runTest(mainDispatcher.dispatcher) {
        val savedState = stateFor(GameMode.HARDCORE)
        val original = newViewModel(GameMode.HARDCORE, savedState)
        original.checkSelection(wordA.cells())
        original.onWordTapped(wordB.word)

        val recreated = newViewModel(GameMode.HARDCORE, savedState)
        assertTrue(recreated.state.value.canUndo)
        recreated.undo()
        recreated.undo()

        assertEquals(board, recreated.state.value.board)
    }

    @Test
    fun `no undo once the secret is found`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = newViewModel(GameMode.HARDCORE)
        viewModel.checkSelection(wordA.cells())
        viewModel.checkSecret(board.hiddenWord)

        assertFalse(viewModel.state.value.canUndo)
        assertFalse(viewModel.undo())
    }
}
