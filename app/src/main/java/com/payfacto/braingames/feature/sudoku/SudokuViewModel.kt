package com.payfacto.braingames.feature.sudoku

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.session.SessionState
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.core.undo.UndoStack
import com.payfacto.braingames.feature.sudoku.data.SudokuBoardSaver
import com.payfacto.braingames.feature.sudoku.data.SudokuPuzzleSource
import com.payfacto.braingames.core.model.CheckResult
import com.payfacto.braingames.core.model.FailedCheck
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.feature.sudoku.model.SudokuAssists
import com.payfacto.braingames.feature.sudoku.model.SudokuBoard
import com.payfacto.braingames.feature.sudoku.model.SudokuMove
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
    val mode: GameMode = GameMode.CLASSIC,
    val isNotesMode: Boolean = false,
    val isGenerating: Boolean = false,
    val canUndo: Boolean = false,
    /** Set while the "Not quite right" dialog is open. */
    val failedCheck: FailedCheck? = null,
    /** Cells revealed as wrong by "Show where"; cleared by the next change. */
    val errorCells: Set<Int> = emptySet()
) {
    /** Hardcore's Check button is enabled only once every cell has a value. */
    val canCheck: Boolean get() = !isGenerating && board.isFilled()
}

@HiltViewModel
class SudokuViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val puzzleSource: SudokuPuzzleSource,
    repository: PreferencesRepository,
    timeSource: TimeSource
) : ViewModel() {

    private val difficulty = Difficulty.fromRouteArg(savedStateHandle[Screen.ARG_DIFFICULTY])
    private val mode = GameMode.fromName(savedStateHandle[Screen.ARG_MODE])
    private val assists = SudokuAssists.forMode(mode)
    private val gameSession = GameSession(
        scope = viewModelScope,
        savedState = savedStateHandle,
        preferences = repository,
        maxHints = assists.maxHints,
        timeSource = timeSource
    )
    private val undoStack: UndoStack<SudokuMove> =
        UndoStack(initial = SudokuBoardSaver.restoreUndo(savedStateHandle))

    private val _state = MutableStateFlow(
        SudokuUiState(difficulty = difficulty, mode = mode, canUndo = undoStack.canUndo)
    )
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
        undoStack.clear()
        SudokuBoardSaver.clear(savedStateHandle)
        _state.update {
            it.copy(
                isGenerating = true,
                selectedCell = -1,
                isNotesMode = false,
                canUndo = false,
                failedCheck = null,
                errorCells = emptySet()
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
        val board = state.board

        if (state.isNotesMode) {
            val notes = board.notes[idx]
            changeCell(idx, board.current[idx], if (number in notes) notes - number else notes + number)
            return
        }

        val notes = if (assists.clearCellNotesOnEntry) emptySet() else board.notes[idx]
        changeCell(idx, number, notes)?.let(::completeIfSolved)
    }

    fun clearCell() {
        val state = _state.value
        val idx = state.selectedCell
        if (idx < 0 || state.board.given[idx]) return
        changeCell(idx, value = 0, notes = emptySet())
    }

    /** In Classic a hint is undoable too; the hint itself stays spent. */
    fun useHint(): Boolean {
        if (!assists.hintsAllowed || !gameSession.hasHints) return false
        val board = _state.value.board
        val idx = (0 until 81).firstOrNull { i ->
            !board.given[i] && board.current[i] != board.solution[i]
        } ?: return false
        gameSession.consumeHint()
        val newBoard = changeCell(idx, board.solution[idx], emptySet()) ?: return true
        _state.update { it.copy(selectedCell = idx) }
        completeIfSolved(newBoard)
        return true
    }

    /** Puts back the cell changed last and selects it. */
    fun undo() {
        val move = undoStack.pop() ?: return
        val newBoard = _state.value.board.withCell(move.index, move.value, move.notes)
        commit(newBoard)
        _state.update { it.copy(selectedCell = move.index) }
        completeIfSolved(newBoard)
    }

    /**
     * Compares the grid with the solution. Solved stops the clock and shows the win screen;
     * Incorrect opens the "Not quite right" dialog. A failed check costs nothing.
     */
    fun checkSolution(): CheckResult {
        val wrongCells = _state.value.board.wrongCells()
        if (wrongCells.isEmpty()) {
            _state.update { it.copy(failedCheck = null, errorCells = emptySet()) }
            gameSession.complete()
            return CheckResult.Solved
        }
        _state.update { it.copy(failedCheck = FailedCheck(wrongCells)) }
        return CheckResult.Incorrect(wrongCells)
    }

    /** "Keep going": closes the dialog without revealing anything. */
    fun dismissFailedCheck() {
        _state.update { it.copy(failedCheck = null) }
    }

    /** "Show how many": the dialog stays open and now shows the count. */
    fun showWrongCount() {
        _state.update { state -> state.copy(failedCheck = state.failedCheck?.copy(isCountShown = true)) }
    }

    /** "Show where": closes the dialog and marks the wrong cells until the next change. */
    fun showWrongCells() {
        _state.update { state ->
            state.copy(failedCheck = null, errorCells = state.failedCheck?.wrongCells.orEmpty())
        }
    }

    fun dismissWin() = gameSession.dismissWin()

    fun toggleMute() = gameSession.toggleMute()

    /** Call when the game screen becomes visible / hidden, so the clock pauses in the background. */
    fun onScreenVisible() = gameSession.onForeground()
    fun onScreenHidden() = gameSession.onBackground()

    /**
     * The only way a player move changes the board: records the cell's previous state for
     * undo, then saves. Returns the new board, or null when nothing actually changed.
     */
    private fun changeCell(index: Int, value: Int, notes: Set<Int>): SudokuBoard? {
        val board = _state.value.board
        if (board.current[index] == value && board.notes[index] == notes) return null
        undoStack.push(board.moveAt(index))
        return board.withCell(index, value, notes).also(::commit)
    }

    /** Saves a changed board. Any change also clears error marks revealed by a check. */
    private fun commit(board: SudokuBoard) {
        SudokuBoardSaver.save(savedStateHandle, board)
        saveUndo()
        _state.update { it.copy(board = board, canUndo = undoStack.canUndo, errorCells = emptySet()) }
    }

    /** Classic declares the win as soon as the grid is right; Hardcore waits for Check. */
    private fun completeIfSolved(board: SudokuBoard) {
        if (assists.autoDetectWin && board.isComplete()) gameSession.complete()
    }

    private fun saveBoard(board: SudokuBoard) = SudokuBoardSaver.save(savedStateHandle, board)

    private fun saveUndo() = SudokuBoardSaver.saveUndo(savedStateHandle, undoStack.toList())
}
