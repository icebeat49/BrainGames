package com.payfacto.braingames.feature.kakuro

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.session.SessionState
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.kakuro.data.KakuroBoardSaver
import com.payfacto.braingames.feature.kakuro.data.KakuroPuzzleBank
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class KakuroUiState(
    val board: KakuroBoard = KakuroBoard(0, 0, emptyList()),
    val selectedCell: Pair<Int, Int>? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM
)

@HiltViewModel
class KakuroViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    repository: PreferencesRepository
) : ViewModel() {

    private val difficulty = Difficulty.fromRouteArg(savedStateHandle[Screen.ARG_DIFFICULTY])
    private val gameSession = GameSession(viewModelScope, savedStateHandle, repository)

    private val _state = MutableStateFlow(KakuroUiState(difficulty = difficulty))
    val state: StateFlow<KakuroUiState> = _state.asStateFlow()
    val session: StateFlow<SessionState> = gameSession.state

    init {
        val restored = KakuroBoardSaver.restore(savedStateHandle)
        if (restored == null) {
            newPuzzle()
        } else {
            _state.update { it.copy(board = restored) }
        }
    }

    fun newPuzzle() {
        gameSession.reset()
        val board = KakuroPuzzleBank.load(context, difficulty)
        saveBoard(board)
        _state.update { it.copy(board = board, selectedCell = null) }
        gameSession.start()
    }

    fun selectCell(row: Int, col: Int) {
        if (_state.value.board.cellAt(row, col) is KakuroCell.Input) {
            _state.update { it.copy(selectedCell = Pair(row, col)) }
        }
    }

    fun enterNumber(number: Int) {
        val (row, col) = _state.value.selectedCell ?: return
        val newBoard = _state.value.board.withInput(row, col, number)
        saveBoard(newBoard)
        _state.update { it.copy(board = newBoard) }
        if (newBoard.isComplete()) gameSession.complete()
    }

    fun clearCell() {
        val (row, col) = _state.value.selectedCell ?: return
        val newBoard = _state.value.board.withInput(row, col, 0)
        saveBoard(newBoard)
        _state.update { it.copy(board = newBoard) }
    }

    fun useHint(): Boolean {
        if (!gameSession.hasHints) return false
        val board = _state.value.board
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val cell = board.cellAt(r, c)
                if (cell is KakuroCell.Input && cell.current != cell.solution) {
                    gameSession.consumeHint()
                    val newBoard = board.withInput(r, c, cell.solution)
                    saveBoard(newBoard)
                    _state.update { it.copy(board = newBoard, selectedCell = Pair(r, c)) }
                    if (newBoard.isComplete()) gameSession.complete()
                    return true
                }
            }
        }
        return false
    }

    fun dismissWin() = gameSession.dismissWin()

    fun toggleMute() = gameSession.toggleMute()

    /** Call when the game screen becomes visible / hidden, so the clock pauses in the background. */
    fun onScreenVisible() = gameSession.onForeground()
    fun onScreenHidden() = gameSession.onBackground()

    private fun saveBoard(board: KakuroBoard) = KakuroBoardSaver.save(savedStateHandle, board)
}
