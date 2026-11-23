package com.payfacto.braingames.feature.sudoku

import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.testutil.MainDispatcherRule
import com.payfacto.braingames.testutil.TestBoards
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SudokuUndoTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val fixture = SudokuFixture(GameMode.CLASSIC)
    private val emptyBoard = TestBoards.sudoku()

    @Test
    fun `nothing to undo on a fresh puzzle`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }

        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `entry, note and erase are undone in reverse order`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.write(cell = 0, value = 1)
        val afterEntry = viewModel.state.value.board
        viewModel.note(cell = 1, value = 6)
        val afterNote = viewModel.state.value.board
        viewModel.erase(cell = 0)

        viewModel.undo()
        val undoneErase = viewModel.state.value.board
        viewModel.undo()
        val undoneNote = viewModel.state.value.board
        viewModel.undo()
        val undoneEntry = viewModel.state.value.board

        assertEquals(afterNote, undoneErase)
        assertEquals(afterEntry, undoneNote)
        assertEquals(emptyBoard, undoneEntry)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `undo brings back the notes a digit hid`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.note(cell = 2, value = 3)
        viewModel.note(cell = 2, value = 5)
        viewModel.write(cell = 2, value = 3)

        viewModel.undo()

        assertEquals(0, viewModel.state.value.board.current[2])
        assertEquals(setOf(3, 5), viewModel.state.value.board.notes[2])
    }

    @Test
    fun `undo selects the cell it restored`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.write(cell = 3, value = 4)
        viewModel.selectCell(0)

        viewModel.undo()

        assertEquals(3, viewModel.state.value.selectedCell)
    }

    @Test
    fun `a change that changes nothing is not recorded`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }

        viewModel.erase(cell = 0)

        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `a classic hint can be undone but stays spent`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.useHint()

        viewModel.undo()

        assertEquals(emptyBoard, viewModel.state.value.board)
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `the stack is empty after a new puzzle`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(fixture) { newViewModel() }
        viewModel.write(cell = 0, value = 1)
        viewModel.note(cell = 1, value = 2)

        viewModel.newPuzzle()
        advanceUntilIdle()
        viewModel.undo()

        assertFalse(viewModel.state.value.canUndo)
        assertEquals(emptyBoard, viewModel.state.value.board)
    }

    @Test
    fun `undo history survives ViewModel recreation`() = runTest(mainDispatcher.dispatcher) {
        val original = with(fixture) { newViewModel() }
        original.write(cell = 0, value = 1)
        original.note(cell = 1, value = 6)

        val recreated = with(fixture) { newViewModel() }
        val canUndoAfterRecreation = recreated.state.value.canUndo
        recreated.undo()
        recreated.undo()

        assertTrue(canUndoAfterRecreation)
        assertEquals(emptyBoard, recreated.state.value.board)
        assertFalse(recreated.state.value.canUndo)
    }
}
