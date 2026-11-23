package com.payfacto.braingames.feature.sudoku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.sudoku.data.SudokuGenerator
import com.payfacto.braingames.feature.sudoku.model.SudokuBoard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SudokuUiState(
    val board: SudokuBoard = SudokuBoard(),
    val selectedCell: Int = -1,
    val showWin: Boolean = false,
    val hintsRemaining: Int = 5,
    val elapsedSeconds: Long = 0L,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isMuted: Boolean = false,
    val isNotesMode: Boolean = false,
    val isGenerating: Boolean = false
)

@HiltViewModel
class SudokuViewModel @Inject constructor(
    private val repository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SudokuUiState())
    val state: StateFlow<SudokuUiState> = _state.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    fun init(difficulty: Difficulty) {
        _state.update { it.copy(difficulty = difficulty) }
        newPuzzle(difficulty)
        viewModelScope.launch {
            repository.preferences.collect { prefs ->
                _state.update { it.copy(isMuted = prefs.isMuted) }
            }
        }
    }

    fun newPuzzle(difficulty: Difficulty = _state.value.difficulty) {
        timerJob?.cancel()
        _state.update {
            it.copy(
                isGenerating = true,
                showWin = false,
                selectedCell = -1,
                elapsedSeconds = 0L,
                isNotesMode = false,
                difficulty = difficulty
            )
        }
        viewModelScope.launch {
            val board = withContext(Dispatchers.Default) { SudokuGenerator.generate(difficulty) }
            _state.update {
                it.copy(
                    board = board,
                    hintsRemaining = 5,
                    isGenerating = false
                )
            }
            startTimer()
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
            _state.update { it.copy(board = it.board.copy(notes = newNotes)) }
            return
        }

        val newCurrent = state.board.current.copyOf()
        newCurrent[idx] = number
        val newNotes = state.board.notes.copyOf()
        newNotes[idx] = emptySet()
        val newBoard = state.board.copy(current = newCurrent, notes = newNotes)
        val isComplete = newBoard.isComplete()

        _state.update {
            it.copy(
                board = newBoard,
                showWin = isComplete
            )
        }
        if (isComplete) timerJob?.cancel()
    }

    fun clearCell() {
        val state = _state.value
        val idx = state.selectedCell
        if (idx < 0 || state.board.given[idx]) return
        val newCurrent = state.board.current.copyOf()
        val newNotes = state.board.notes.copyOf()
        newCurrent[idx] = 0
        newNotes[idx] = emptySet()
        _state.update { it.copy(board = it.board.copy(current = newCurrent, notes = newNotes)) }
    }

    fun useHint(): Boolean {
        val state = _state.value
        if (state.hintsRemaining <= 0) return false
        val board = state.board
        val idx = (0 until 81).firstOrNull { i ->
            !board.given[i] && board.current[i] != board.solution[i]
        } ?: return false
        val newCurrent = board.current.copyOf()
        newCurrent[idx] = board.solution[idx]
        val newNotes = board.notes.copyOf()
        newNotes[idx] = emptySet()
        val newBoard = board.copy(current = newCurrent, notes = newNotes)
        val isComplete = newBoard.isComplete()
        _state.update {
            it.copy(
                board = newBoard,
                hintsRemaining = it.hintsRemaining - 1,
                selectedCell = idx,
                showWin = isComplete
            )
        }
        if (isComplete) timerJob?.cancel()
        return true
    }

    fun dismissWin() {
        _state.update { it.copy(showWin = false) }
    }

    fun toggleMute() {
        viewModelScope.launch {
            repository.setMuted(!_state.value.isMuted)
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
