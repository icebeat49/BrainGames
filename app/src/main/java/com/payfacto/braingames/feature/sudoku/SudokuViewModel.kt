package com.payfacto.braingames.feature.sudoku

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.session.SessionState
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.sudoku.data.SudokuBoardSaver
import com.payfacto.braingames.feature.sudoku.data.SudokuPuzzleSource
import com.payfacto.braingames.feature.sudoku.model.SudokuBoard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.TimeSource

data class SudokuUiState(
    val board: SudokuBoard = SudokuBoard(),
    val selectedCell: Int = -1,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isNotesMode: Boolean = false,
    val isGenerating: Boolean = false
)

@HiltViewModel
class SudokuViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val puzzleSource: SudokuPuzzleSource,
    repository: PreferencesRepository,
    timeSource: TimeSource
) : ViewModel() {

    private val difficulty = Difficulty.fromRouteArg(savedStateHandle[Screen.ARG_DIFFICULTY])
    private val gameSession =
        GameSession(viewModelScope, savedStateHandle, repository, timeSource = timeSource)

    private val _state = MutableStateFlow(SudokuUiState(difficulty = difficulty))
    val state: StateFlow<SudokuUiState> = _state.asStateFlow()
    val session: StateFlow<SessionState> = gameSession.state

    init {
        val restored = SudokuBoardSaver.restore(savedStateHandle)
        if (restored == null) {
            newPuzzle()
        } else {
            _state.update { it.copy(board = restored) }
        }
    }

    fun newPuzzle() {
        gameSession.reset()
        SudokuBoardSaver.clear(savedStateHandle)
        _state.update {
            it.copy(
                isGenerating = true,
                selectedCell = -1,
                isNotesMode = false
            )
        }
        viewModelScope.launch {
            val board = puzzleSource.newPuzzle(difficulty)
            saveBoard(board)
            _state.update { it.copy(board = board, isGenerating = false) }
            gameSession.start()
        }
    }

    fun selectCell(index: Int) {
        _state.update { it.copy(selectedCell = index) }
    }

    fun toggleNotesMode() {
        _state.update { it.copy(isNotesMode = !it.isNotesMode) }
    }

    fun enterNumber(number: Int) {
        val state = _state.value
        val idx = state.selectedCell
        if (idx < 0 || state.board.given[idx]) return

        if (state.isNotesMode) {
            val currentNotes = state.board.notes[idx]
            val newNotes = state.board.notes.copyOf()
            newNotes[idx] = if (number in currentNotes) currentNotes - number else currentNotes + number
            val newBoard = state.board.copy(notes = newNotes)
            saveBoard(newBoard)
            _state.update { it.copy(board = newBoard) }
            return
        }

        val newCurrent = state.board.current.copyOf()
        newCurrent[idx] = number
        val newNotes = state.board.notes.copyOf()
        newNotes[idx] = emptySet()
        val newBoard = state.board.copy(current = newCurrent, notes = newNotes)
        saveBoard(newBoard)
        _state.update { it.copy(board = newBoard) }
        if (newBoard.isComplete()) gameSession.complete()
    }

    fun clearCell() {
        val state = _state.value
        val idx = state.selectedCell
        if (idx < 0 || state.board.given[idx]) return
        val newCurrent = state.board.current.copyOf()
        val newNotes = state.board.notes.copyOf()
        newCurrent[idx] = 0
        newNotes[idx] = emptySet()
        val newBoard = state.board.copy(current = newCurrent, notes = newNotes)
        saveBoard(newBoard)
        _state.update { it.copy(board = newBoard) }
    }

    fun useHint(): Boolean {
        if (!gameSession.hasHints) return false
        val board = _state.value.board
        val idx = (0 until 81).firstOrNull { i ->
            !board.given[i] && board.current[i] != board.solution[i]
        } ?: return false
        gameSession.consumeHint()
        val newCurrent = board.current.copyOf()
        newCurrent[idx] = board.solution[idx]
        val newNotes = board.notes.copyOf()
        newNotes[idx] = emptySet()
        val newBoard = board.copy(current = newCurrent, notes = newNotes)
        saveBoard(newBoard)
        _state.update { it.copy(board = newBoard, selectedCell = idx) }
        if (newBoard.isComplete()) gameSession.complete()
        return true
    }

    fun dismissWin() = gameSession.dismissWin()

    fun toggleMute() = gameSession.toggleMute()

    /** Call when the game screen becomes visible / hidden, so the clock pauses in the background. */
    fun onScreenVisible() = gameSession.onForeground()
    fun onScreenHidden() = gameSession.onBackground()

    private fun saveBoard(board: SudokuBoard) = SudokuBoardSaver.save(savedStateHandle, board)
}
