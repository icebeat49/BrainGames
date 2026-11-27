package com.payfacto.braingames.feature.wordsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.session.SessionState
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.wordsearch.data.WordSearchBoardSaver
import com.payfacto.braingames.feature.wordsearch.data.WordSearchPuzzleSource
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

data class WordSearchUiState(
    val board: WordSearchBoard = WordSearchBoard(Array(8) { CharArray(8) }, emptyList()),
    val hintCell: Pair<Int, Int>? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isLoading: Boolean = false
)

@HiltViewModel
class WordSearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val puzzleSource: WordSearchPuzzleSource,
    repository: PreferencesRepository,
    timeSource: TimeSource
) : ViewModel() {

    private val difficulty = Difficulty.fromRouteArg(savedStateHandle[Screen.ARG_DIFFICULTY])
    private val gameSession =
        GameSession(viewModelScope, savedStateHandle, repository, timeSource = timeSource)

    private var puzzleLoading: Job? = null
    private val _state = MutableStateFlow(WordSearchUiState(difficulty = difficulty))
    val state: StateFlow<WordSearchUiState> = _state.asStateFlow()
    val session: StateFlow<SessionState> = gameSession.state

    init {
        val restored = WordSearchBoardSaver.restore(savedStateHandle)
        if (restored == null) {
            newPuzzle()
        } else {
            _state.update { it.copy(board = restored) }
        }
    }

    fun newPuzzle() {
        gameSession.reset()
        WordSearchBoardSaver.clear(savedStateHandle)
        _state.update { it.copy(isLoading = true) }
        // A second tap before the first puzzle is ready replaces it, so only one puzzle loads.
        puzzleLoading?.cancel()
        puzzleLoading = viewModelScope.launch {
            val board = puzzleSource.newPuzzle(difficulty)
            saveBoard(board)
            _state.update { it.copy(board = board, hintCell = null, isLoading = false) }
            gameSession.start()
        }
    }

    fun checkSelection(cells: List<Pair<Int, Int>>): Boolean {
        if (cells.size < 2) return false
        val board = _state.value.board
        val selected = cells.map { (r, c) -> board.grid[r][c] }.joinToString("")
        val reversed = selected.reversed()

        val matchIdx = board.words.indexOfFirst { entry ->
            !entry.found && (entry.word == selected || entry.word == reversed)
        }
        if (matchIdx < 0) return false

        val newWords = board.words.toMutableList()
        newWords[matchIdx] = newWords[matchIdx].copy(found = true)
        val newBoard = board.copy(words = newWords)
        saveBoard(newBoard)
        _state.update { it.copy(board = newBoard, hintCell = null) }

        // Leave time to see the revealed hidden word before the win screen appears.
        if (newBoard.isComplete()) gameSession.complete(winDelay = WIN_REVEAL_DELAY)
        return true
    }

    fun useHint(): Boolean {
        if (!gameSession.hasHints) return false
        val unfound = _state.value.board.words.firstOrNull { !it.found } ?: return false
        gameSession.consumeHint()
        _state.update { it.copy(hintCell = Pair(unfound.startRow, unfound.startCol)) }
        viewModelScope.launch {
            delay(HINT_DISPLAY_DURATION)
            _state.update { it.copy(hintCell = null) }
        }
        return true
    }

    fun dismissWin() = gameSession.dismissWin()

    fun toggleMute() = gameSession.toggleMute()

    /** Call when the game screen becomes visible / hidden, so the clock pauses in the background. */
    fun onScreenVisible() = gameSession.onForeground()
    fun onScreenHidden() = gameSession.onBackground()

    private fun saveBoard(board: WordSearchBoard) = WordSearchBoardSaver.save(savedStateHandle, board)

    private companion object {
        val WIN_REVEAL_DELAY = 2.seconds
        val HINT_DISPLAY_DURATION = 2.seconds
    }
}
