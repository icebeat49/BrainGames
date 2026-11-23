package com.payfacto.braingames.feature.wordsearch

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.wordsearch.data.FallbackPuzzles
import com.payfacto.braingames.feature.wordsearch.data.PuzzleBank
import com.payfacto.braingames.feature.wordsearch.data.WordBank
import com.payfacto.braingames.feature.wordsearch.data.WordSearchBoardSaver
import com.payfacto.braingames.feature.wordsearch.data.WordSearchGenerator
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WordSearchUiState(
    val board: WordSearchBoard = WordSearchBoard(Array(8) { CharArray(8) }, emptyList()),
    val hintCell: Pair<Int, Int>? = null,
    val showWin: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isMuted: Boolean = false,
    val hintsRemaining: Int = 5,
    val isLoading: Boolean = false
)

@HiltViewModel
class WordSearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val repository: PreferencesRepository
) : ViewModel() {

    private val difficulty = Difficulty.fromRouteArg(savedStateHandle[Screen.ARG_DIFFICULTY])

    private val _state = MutableStateFlow(WordSearchUiState(difficulty = difficulty))
    val state: StateFlow<WordSearchUiState> = _state.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            repository.preferences.map { it.isMuted }.distinctUntilChanged().collect { muted ->
                _state.update { it.copy(isMuted = muted) }
            }
        }
        val restored = WordSearchBoardSaver.restore(savedStateHandle)
        if (restored == null) {
            newPuzzle()
        } else {
            _state.update { it.copy(board = restored) }
            if (!restored.isComplete()) startTimer()
        }
    }

    fun newPuzzle() {
        timerJob?.cancel()
        WordSearchBoardSaver.clear(savedStateHandle)
        _state.update { it.copy(isLoading = true, showWin = false) }
        viewModelScope.launch {
            val board = withContext(Dispatchers.Default) {
                val gridSize = WordSearchGenerator.gridSizeFor(difficulty)
                val maxWordLen = gridSize.coerceAtMost(12)
                val words = WordBank.getWords(context, maxWordLen)
                val candidates = PuzzleBank.getPuzzles(context).shuffled()

                // Try each candidate puzzle until one yields a verified board (no random
                // filler is ever produced — a board either satisfies the rules or is null).
                candidates.firstNotNullOfOrNull { puzzle ->
                    WordSearchGenerator.generate(words, puzzle, difficulty)
                } ?: FallbackPuzzles.get(context, difficulty)
            }
            saveBoard(board)
            _state.update {
                it.copy(
                    board = board,
                    hintCell = null,
                    showWin = false,
                    elapsedSeconds = 0L,
                    hintsRemaining = 5,
                    isLoading = false
                )
            }
            startTimer()
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
        val isComplete = newBoard.isComplete()
        saveBoard(newBoard)

        _state.update { it.copy(board = newBoard, hintCell = null) }

        if (isComplete) {
            timerJob?.cancel()
            viewModelScope.launch {
                delay(2_000)
                _state.update { it.copy(showWin = true) }
            }
        }
        return true
    }

    fun useHint(): Boolean {
        val state = _state.value
        if (state.hintsRemaining <= 0) return false
        val unfound = state.board.words.firstOrNull { !it.found } ?: return false
        _state.update {
            it.copy(
                hintCell = Pair(unfound.startRow, unfound.startCol),
                hintsRemaining = it.hintsRemaining - 1
            )
        }
        viewModelScope.launch {
            delay(2_000)
            _state.update { it.copy(hintCell = null) }
        }
        return true
    }

    fun dismissWin() { _state.update { it.copy(showWin = false) } }

    fun toggleMute() {
        viewModelScope.launch { repository.setMuted(!_state.value.isMuted) }
    }

    private fun saveBoard(board: WordSearchBoard) = WordSearchBoardSaver.save(savedStateHandle, board)

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
