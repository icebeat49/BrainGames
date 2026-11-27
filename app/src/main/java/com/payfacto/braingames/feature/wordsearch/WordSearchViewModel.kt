package com.payfacto.braingames.feature.wordsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.session.SessionState
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.core.undo.UndoStack
import com.payfacto.braingames.feature.wordsearch.data.WordSearchBoardSaver
import com.payfacto.braingames.feature.wordsearch.data.WordSearchPuzzleSource
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard
import com.payfacto.braingames.feature.wordsearch.model.WordSearchEdit
import com.payfacto.braingames.feature.wordsearch.model.applying
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
    val board: WordSearchBoard = WordSearchBoard(Array(12) { CharArray(12) }, emptyList()),
    val hintCell: Pair<Int, Int>? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val mode: GameMode = GameMode.CLASSIC,
    val canUndo: Boolean = false,
    val isLoading: Boolean = false
)

/** What a trace in the grid did. */
enum class SelectionResult {
    /** Classic: a listed word, crossed off. */
    FOUND,
    /** Classic: not a word still to find. */
    NOT_A_WORD,
    /** Hardcore: a pencil line drawn, unchecked. */
    DRAWN,
    /** Hardcore: the same line traced again, so it was erased. */
    ERASED,
    /** Too short to mean anything (a tap): ignored. */
    IGNORED,
}

@HiltViewModel
class WordSearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val puzzleSource: WordSearchPuzzleSource,
    repository: PreferencesRepository,
    timeSource: TimeSource
) : ViewModel() {

    private val difficulty = Difficulty.fromRouteArg(savedStateHandle[Screen.ARG_DIFFICULTY])
    private val mode = GameMode.fromName(savedStateHandle[Screen.ARG_MODE])
    private val isClassic = mode == GameMode.CLASSIC
    private val gameSession = GameSession(
        scope = viewModelScope,
        savedState = savedStateHandle,
        preferences = repository,
        maxHints = if (isClassic) GameSession.DEFAULT_HINTS else 0,
        timeSource = timeSource
    )

    private var puzzleLoading: Job? = null
    /** Each step is the edits one action made; undoing applies them again, last first. */
    private var undoStack = UndoStack<List<WordSearchEdit>>(UNDO_LIMIT)
    private val _state = MutableStateFlow(WordSearchUiState(difficulty = difficulty, mode = mode))
    val state: StateFlow<WordSearchUiState> = _state.asStateFlow()
    val session: StateFlow<SessionState> = gameSession.state

    init {
        val restored = WordSearchBoardSaver.restore(savedStateHandle)
        if (restored == null) {
            newPuzzle()
        } else {
            undoStack = UndoStack(UNDO_LIMIT, WordSearchBoardSaver.restoreUndo(savedStateHandle))
            _state.update { it.copy(board = restored, canUndo = undoStack.canUndo) }
        }
    }

    fun newPuzzle() {
        gameSession.reset()
        WordSearchBoardSaver.clear(savedStateHandle)
        undoStack.clear()
        _state.update { it.copy(isLoading = true, canUndo = false) }
        // A second tap before the first puzzle is ready replaces it, so only one puzzle loads.
        puzzleLoading?.cancel()
        puzzleLoading = viewModelScope.launch {
            val board = puzzleSource.newPuzzle(difficulty)
            saveBoard(board)
            _state.update { it.copy(board = board, hintCell = null, isLoading = false) }
            gameSession.start()
        }
    }

    /** A trace from the first to the last cell of [cells], in a straight line. */
    fun checkSelection(cells: List<Pair<Int, Int>>): SelectionResult {
        if (cells.size < 2) return SelectionResult.IGNORED
        return if (isClassic) findWord(cells) else toggleLine(cells)
    }

    private fun findWord(cells: List<Pair<Int, Int>>): SelectionResult {
        val board = _state.value.board
        val selected = cells.map { (r, c) -> board.grid[r][c] }.joinToString("")
        val match = board.words.firstOrNull { entry ->
            !entry.found && (entry.word == selected || entry.word == selected.reversed())
        } ?: return SelectionResult.NOT_A_WORD
        edit(listOf(WordSearchEdit.TickWord(match.word), WordSearchEdit.ToggleLine(match.cells())))
        return SelectionResult.FOUND
    }

    /** Hardcore, like pencil on paper: any line can be drawn, and tracing it again erases it. */
    private fun toggleLine(cells: List<Pair<Int, Int>>): SelectionResult {
        val linesBefore = _state.value.board.marks.size
        edit(listOf(WordSearchEdit.ToggleLine(cells)))
        return if (_state.value.board.marks.size > linesBefore) SelectionResult.DRAWN else SelectionResult.ERASED
    }

    /** Hardcore: ticks a word off the list, or back on, at any time. Classic ignores taps. */
    fun onWordTapped(word: String) {
        val board = _state.value.board
        if (isClassic || board.secretFound) return
        if (board.words.none { it.word == word }) return
        edit(listOf(WordSearchEdit.TickWord(word)))
    }

    /** Takes back the last word found (Classic), line drawn or erased, or word ticked. */
    fun undo(): Boolean {
        if (_state.value.board.secretFound) return false
        val step = undoStack.pop() ?: return false
        apply(_state.value.board.applying(step.asReversed()))
        return true
    }

    /** The one path for player changes: records the step for undo, then saves and shows it. */
    private fun edit(edits: List<WordSearchEdit>) {
        undoStack.push(edits)
        apply(_state.value.board.applying(edits))
    }

    private fun apply(board: WordSearchBoard) {
        WordSearchBoardSaver.saveUndo(savedStateHandle, undoStack.toList())
        updateBoard(board)
    }

    /** The game is won by typing the secret, whether or not every word was found first. */
    fun checkSecret(guess: String): Boolean {
        val board = _state.value.board
        if (board.secretFound || !board.isSecret(guess)) return false
        // A won game has nothing to take back.
        undoStack.clear()
        WordSearchBoardSaver.saveUndo(savedStateHandle, emptyList())
        updateBoard(board.copy(secretFound = true))
        // Leave time to see the secret revealed in the grid before the win screen appears.
        gameSession.complete(winDelay = WIN_REVEAL_DELAY)
        return true
    }

    private fun updateBoard(board: WordSearchBoard) {
        saveBoard(board)
        _state.update { it.copy(board = board, hintCell = null, canUndo = undoStack.canUndo) }
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
        const val UNDO_LIMIT = 100
    }
}
