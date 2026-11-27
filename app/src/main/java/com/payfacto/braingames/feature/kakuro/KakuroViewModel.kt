package com.payfacto.braingames.feature.kakuro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.save.GameSaveRecorder
import com.payfacto.braingames.core.save.GameSaves
import com.payfacto.braingames.core.stats.GameStatsRecorder
import com.payfacto.braingames.core.stats.StatsKey
import com.payfacto.braingames.core.stats.StatsStore
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.session.SessionState
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.kakuro.data.KakuroBoardSaver
import com.payfacto.braingames.feature.kakuro.data.KakuroPuzzleSource
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.TimeSource

data class KakuroUiState(
    val board: KakuroBoard = KakuroBoard(0, 0, emptyList()),
    val selectedCell: Pair<Int, Int>? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM
)

@HiltViewModel
class KakuroViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val puzzleSource: KakuroPuzzleSource,
    repository: PreferencesRepository,
    timeSource: TimeSource,
    saves: GameSaves,
    stats: StatsStore
) : ViewModel() {

    private val difficulty = Difficulty.fromRouteArg(savedStateHandle[Screen.ARG_DIFFICULTY])
    private val gameSession =
        GameSession(viewModelScope, savedStateHandle, repository, timeSource = timeSource)
    /** Kakuro has no Hardcore mode yet, so its save is always Classic. */
    private val recorder = GameSaveRecorder(
        GameType.KAKURO, savedStateHandle, saves, difficulty, GameMode.CLASSIC,
        KakuroBoardSaver.KEYS + GameStatsRecorder.KEYS
    )
    private val statsRecorder =
        GameStatsRecorder(StatsKey(GameType.KAKURO, difficulty, GameMode.CLASSIC), savedStateHandle, stats)

    private val _state = MutableStateFlow(KakuroUiState(difficulty = difficulty))
    val state: StateFlow<KakuroUiState> = _state.asStateFlow()
    val session: StateFlow<SessionState> = gameSession.state

    init {
        when {
            restoreGame() -> Unit
            savedStateHandle.get<Boolean>(Screen.ARG_RESUME) == true -> resumeSavedGame()
            else -> newPuzzle()
        }
    }

    fun newPuzzle() {
        gameSession.reset()
        recorder.clear()
        statsRecorder.onNewPuzzle()
        KakuroBoardSaver.clear(savedStateHandle)
        _state.update { it.copy(board = EMPTY_BOARD, selectedCell = null) }
        viewModelScope.launch {
            val board = puzzleSource.newPuzzle(difficulty)
            saveBoard(board)
            _state.update { it.copy(board = board) }
            gameSession.start()
            recorder.record()
        }
    }

    /** Continue: loads the game saved on disk, or a new puzzle if it is gone. */
    private fun resumeSavedGame() {
        viewModelScope.launch {
            if (recorder.restoreInto() && restoreGame()) gameSession.reloadSaved() else newPuzzle()
        }
    }

    /** Shows the board kept in the SavedStateHandle. Returns false when it holds none. */
    private fun restoreGame(): Boolean {
        val board = KakuroBoardSaver.restore(savedStateHandle) ?: return false
        _state.update { it.copy(board = board) }
        return true
    }

    fun selectCell(row: Int, col: Int) {
        val board = _state.value.board
        if (row !in 0 until board.rows || col !in 0 until board.cols) return
        if (board.cellAt(row, col) is KakuroCell.Input) {
            _state.update { it.copy(selectedCell = Pair(row, col)) }
        }
    }

    fun enterNumber(number: Int) {
        val (row, col) = _state.value.selectedCell ?: return
        val newBoard = _state.value.board.withInput(row, col, number)
        saveMove(newBoard)
        _state.update { it.copy(board = newBoard) }
        if (newBoard.isComplete()) win()
    }

    fun clearCell() {
        val (row, col) = _state.value.selectedCell ?: return
        val newBoard = _state.value.board.withInput(row, col, 0)
        saveMove(newBoard)
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
                    saveMove(newBoard)
                    _state.update { it.copy(board = newBoard, selectedCell = Pair(r, c)) }
                    if (newBoard.isComplete()) win()
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
    fun onScreenHidden() {
        gameSession.onBackground()
        recorder.record()
    }

    /** A change made by the player: the puzzle now counts as played. */
    private fun saveMove(board: KakuroBoard) {
        statsRecorder.onMove()
        saveBoard(board)
    }

    /** Every board change goes through here: kept for rotation and process death, and on disk. */
    private fun saveBoard(board: KakuroBoard) {
        KakuroBoardSaver.save(savedStateHandle, board)
        recorder.record()
    }

    /** A won game is over: it counts in the stats and its save is deleted. */
    private fun win() {
        gameSession.complete()
        statsRecorder.onWin(gameSession.elapsedMs)
        recorder.clear()
    }

    private companion object {
        /** Shown, and ignored by input, while the next puzzle loads. */
        val EMPTY_BOARD = KakuroBoard(0, 0, emptyList())
    }
}
