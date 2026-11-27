package com.payfacto.braingames.feature.sudoku

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.feature.sudoku.data.SudokuBoardSaver
import com.payfacto.braingames.feature.sudoku.model.SudokuMove
import com.payfacto.braingames.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Classic removes a placed digit from the notes of the cells it sees; Hardcore leaves notes to
 * the player. The test board's open cells 0–4 all share row 0.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SudokuPeerNotesTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    /** Notes on cells 1 and 2 holding the digit about to be written in cell 0, plus another one. */
    private fun SudokuViewModel.noteAround(digit: Int, other: Int) {
        note(cell = 1, value = digit)
        note(cell = 1, value = other)
        note(cell = 2, value = digit)
    }

    @Test
    fun `Classic removes a placed digit from the notes of its row, column and box`() =
        runTest(mainDispatcher.dispatcher) {
            val fixture = SudokuFixture(GameMode.CLASSIC)
            val viewModel = with(fixture) { newViewModel() }
            val digit = fixture.solution[0]
            viewModel.noteAround(digit, other = fixture.solution[1])

            viewModel.write(cell = 0, value = digit)

            val notes = viewModel.state.value.board.notes
            assertEquals(setOf(fixture.solution[1]), notes[1])
            assertEquals(emptySet<Int>(), notes[2])
        }

    @Test
    fun `one undo brings back the digit and the notes it removed`() = runTest(mainDispatcher.dispatcher) {
        val fixture = SudokuFixture(GameMode.CLASSIC)
        val viewModel = with(fixture) { newViewModel() }
        val digit = fixture.solution[0]
        viewModel.noteAround(digit, other = fixture.solution[1])
        val before = viewModel.state.value.board

        viewModel.write(cell = 0, value = digit)
        viewModel.undo()

        assertEquals(before, viewModel.state.value.board)
    }

    @Test
    fun `Hardcore leaves the other cells' notes alone`() = runTest(mainDispatcher.dispatcher) {
        val fixture = SudokuFixture(GameMode.HARDCORE)
        val viewModel = with(fixture) { newViewModel() }
        val digit = fixture.solution[0]
        viewModel.noteAround(digit, other = fixture.solution[1])

        viewModel.write(cell = 0, value = digit)

        assertEquals(setOf(digit), viewModel.state.value.board.notes[2])
    }

    @Test
    fun `linked undo moves survive a save`() {
        val handle = SavedStateHandle()
        val moves = listOf(SudokuMove(0, 0, setOf(1, 9)), SudokuMove(1, 0, setOf(3), linkedToPrevious = true))

        SudokuBoardSaver.saveUndo(handle, moves)

        assertEquals(moves, SudokuBoardSaver.restoreUndo(handle))
    }
}
