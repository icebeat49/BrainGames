package com.payfacto.braingames.feature.kakuro

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.kakuro.data.KakuroBoardSaver
import com.payfacto.braingames.feature.kakuro.data.KakuroPuzzleBank
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KakuroUiState(
    val board: KakuroBoard = KakuroBoard(0, 0, emptyList()),
    val selectedCell: Pair<Int, Int>? = null,
    val showWin: Boolean = false,
    val hintsRemaining: Int = 5,
    val elapsedSeconds: Long = 0L,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isMuted: Boolean = false
)

@HiltViewModel
class KakuroViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val repository: PreferencesRepository
) : ViewModel() {

    private val difficulty = Difficulty.fromRouteArg(savedStateHandle[Screen.ARG_DIFFICULTY])

    private val _state = MutableStateFlow(KakuroUiState(difficulty = difficulty))
    val state: StateFlow<KakuroUiState> = _state.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            repository.preferences.map { it.isMuted }.distinctUntilChanged().collect { muted ->
                _state.update { it.copy(isMuted = muted) }
            }
        }
        val restored = KakuroBoardSaver.restore(savedStateHandle)
        if (restored == null) {
            newPuzzle()
        } else {
            _state.update { it.copy(board = restored) }
            if (!restored.isComplete()) startTimer()
        }
    }

    fun newPuzzle() {
        val board = KakuroPuzzleBank.load(context, difficulty)
        timerJob?.cancel()
        saveBoard(board)
        _state.update {
            it.copy(
                board = board,
                selectedCell = null,
                showWin = false,
                hintsRemaining = 5,
                elapsedSeconds = 0L
            )
        }
        startTimer()
    }

    fun selectCell(row: Int, col: Int) {
        if (_state.value.board.cellAt(row, col) is KakuroCell.Input) {
            _state.update { it.copy(selectedCell = Pair(row, col)) }
        }
    }

    fun enterNumber(number: Int) {
        val (row, col) = _state.value.selectedCell ?: return
        val newBoard = _state.value.board.withInput(row, col, number)
        val isComplete = newBoard.isComplete()
        saveBoard(newBoard)
        _state.update { it.copy(board = newBoard, showWin = isComplete) }
        if (isComplete) timerJob?.cancel()
    }

    fun clearCell() {
        val (row, col) = _state.value.selectedCell ?: return
        val newBoard = _state.value.board.withInput(row, col, 0)
        saveBoard(newBoard)
        _state.update { it.copy(board = newBoard) }
    }

    fun useHint(): Boolean {
        val state = _state.value
        if (state.hintsRemaining <= 0) return false
        val board = state.board
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val cell = board.cellAt(r, c)
                if (cell is KakuroCell.Input && cell.current != cell.solution) {
                    val newBoard = board.withInput(r, c, cell.solution)
                    val isComplete = newBoard.isComplete()
                    saveBoard(newBoard)
                    _state.update {
                        it.copy(
                            board = newBoard,
                            hintsRemaining = it.hintsRemaining - 1,
                            selectedCell = Pair(r, c),
                            showWin = isComplete
                        )
                    }
                    if (isComplete) timerJob?.cancel()
                    return true
                }
            }
        }
        return false
    }

    fun dismissWin() { _state.update { it.copy(showWin = false) } }

    fun toggleMute() {
        viewModelScope.launch { repository.setMuted(!_state.value.isMuted) }
    }

    private fun saveBoard(board: KakuroBoard) = KakuroBoardSaver.save(savedStateHandle, board)

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
