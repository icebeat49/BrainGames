package com.payfacto.braingames.feature.animals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.CheckResult
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.FailedCheck
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.session.SessionState
import com.payfacto.braingames.core.ui.grid.GridPoint
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.core.undo.UndoStack
import com.payfacto.braingames.feature.animals.data.AnimalsGameSaver
import com.payfacto.braingames.feature.animals.data.AnimalsPuzzleSource
import com.payfacto.braingames.feature.animals.engine.AnimalsHint
import com.payfacto.braingames.feature.animals.engine.AnimalsHintEngine
import com.payfacto.braingames.feature.animals.engine.AnimalsValidator
import com.payfacto.braingames.feature.animals.engine.cellsOf
import com.payfacto.braingames.feature.animals.input.AnimalsGestures
import com.payfacto.braingames.feature.animals.input.BoardEdit
import com.payfacto.braingames.feature.animals.model.AnimalsBoard
import com.payfacto.braingames.feature.animals.model.AnimalsPuzzle
import com.payfacto.braingames.feature.animals.model.CellMark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.TimeSource

/** The hint on screen: what it says and which cells it is about. */
data class HintDisplay(
    val hint: AnimalsHint,
    /** Cells the hint changed or points at. */
    val cells: Set<Int>,
    /** Cells of the rows, columns or regions the reason mentions. */
    val areaCells: Set<Int>
)

data class AnimalsUiState(
    val puzzle: AnimalsPuzzle? = null,
    val board: AnimalsBoard = AnimalsBoard.empty(0),
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val mode: GameMode = GameMode.CLASSIC,
    val isGenerating: Boolean = false,
    /** The ✕ / ? toggle: what taps and drags write on empty cells. */
    val isQuestionMode: Boolean = false,
    val canUndo: Boolean = false,
    /** Classic only: animals breaking a rule. */
    val conflictCells: Set<Int> = emptySet(),
    val hint: HintDisplay? = null,
    /** Hardcore: set while the "Not quite right" dialog is open. */
    val failedCheck: FailedCheck? = null,
    /** Hardcore: wrong animals revealed by "Show where", until one of them changes. */
    val errorCells: Set<Int> = emptySet()
) {
    val isPlayable: Boolean get() = puzzle != null && !isGenerating

    /** Hardcore's Check button: enabled once N animals are on the board. */
    val canCheck: Boolean get() = isPlayable && board.animals.size == puzzle?.size
}

@HiltViewModel
class AnimalsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val puzzleSource: AnimalsPuzzleSource,
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
    private val gestures = AnimalsGestures()
    private var undoStack = UndoStack<AnimalsBoard>(UNDO_LIMIT)

    private val _state = MutableStateFlow(
        AnimalsUiState(
            difficulty = difficulty,
            mode = mode,
            isQuestionMode = AnimalsGameSaver.restoreQuestionMode(savedStateHandle)
        )
    )
    val state: StateFlow<AnimalsUiState> = _state.asStateFlow()
    val session: StateFlow<SessionState> = gameSession.state

    init {
        val puzzle = AnimalsGameSaver.restorePuzzle(savedStateHandle)
        if (puzzle == null) {
            newPuzzle()
        } else {
            val board = AnimalsGameSaver.restoreBoard(savedStateHandle, puzzle.size)
            undoStack = UndoStack(UNDO_LIMIT, AnimalsGameSaver.restoreUndo(savedStateHandle, puzzle.size))
            _state.update {
                it.copy(
                    puzzle = puzzle,
                    board = board,
                    canUndo = undoStack.canUndo,
                    conflictCells = conflictsOf(puzzle, board)
                )
            }
        }
    }

    fun newPuzzle() {
        gameSession.reset()
        undoStack.clear()
        AnimalsGameSaver.clear(savedStateHandle)
        _state.update {
            it.copy(
                isGenerating = true,
                canUndo = false,
                conflictCells = emptySet(),
                hint = null,
                failedCheck = null,
                errorCells = emptySet()
            )
        }
        viewModelScope.launch {
            val puzzle = puzzleSource.newPuzzle(difficulty)
            val board = AnimalsBoard.empty(puzzle.size)
            AnimalsGameSaver.savePuzzle(savedStateHandle, puzzle)
            AnimalsGameSaver.saveBoard(savedStateHandle, board)
            _state.update { it.copy(puzzle = puzzle, board = board, isGenerating = false) }
            gameSession.start()
        }
    }

    // ── Gestures (see AnimalsGestures for the rules) ────────────────────────

    fun onTap(cell: Int, downTimeMillis: Long, upTimeMillis: Long, doubleTapTimeoutMillis: Long) {
        val state = _state.value
        if (!state.isPlayable) return
        applyEdit(
            gestures.tap(cell, downTimeMillis, upTimeMillis, doubleTapTimeoutMillis, state.board, state.isQuestionMode)
        )
    }

    fun onDragStart(start: GridPoint) {
        val state = _state.value
        if (!state.isPlayable) return
        applyEdit(gestures.dragStart(start, state.board, state.isQuestionMode))
    }

    fun onDragMove(to: GridPoint) {
        if (!_state.value.isPlayable) return
        applyEdit(gestures.dragMove(to, _state.value.board))
    }

    fun onDragEnd() = gestures.dragEnd()

    /** Direct edit for accessibility actions (TalkBack), as one undo step. */
    fun setMark(cell: Int, mark: CellMark) {
        val state = _state.value
        if (!state.isPlayable) return
        applyChange(state.board.withMark(cell, mark), recordUndo = true)
    }

    fun toggleQuestionMode() {
        val enabled = !_state.value.isQuestionMode
        AnimalsGameSaver.saveQuestionMode(savedStateHandle, enabled)
        _state.update { it.copy(isQuestionMode = enabled) }
    }

    // ── Controls ────────────────────────────────────────────────────────────

    fun undo() {
        val previous = undoStack.pop() ?: return
        applyChange(previous, recordUndo = false)
    }

    /** Removes every mark, as one undoable step. */
    fun clearAll() {
        val state = _state.value
        if (!state.isPlayable) return
        applyChange(AnimalsBoard.empty(state.board.size), recordUndo = true)
    }

    /**
     * Classic only. Applies the next hint as one undoable step (a wrong mark is removed,
     * a deduction is written, a reveal places its animal) and shows what it was about.
     */
    fun useHint(): Boolean {
        val state = _state.value
        val puzzle = state.puzzle ?: return false
        if (!isClassic || !state.isPlayable || !gameSession.hasHints) return false
        val board = state.board
        val hint = AnimalsHintEngine.nextHint(puzzle, board)
        val hinted = when (hint) {
            AnimalsHint.Solved -> return false
            is AnimalsHint.WrongMark -> board.withMark(hint.cell, CellMark.EMPTY)
            is AnimalsHint.Deduction -> board
                .withMarks(hint.step.eliminations, CellMark.CROSS)
                .withMarks(hint.step.placements, CellMark.ANIMAL)
            is AnimalsHint.Reveal -> board.withMark(hint.cell, CellMark.ANIMAL)
        }
        gameSession.consumeHint()
        applyChange(hinted, recordUndo = true)
        _state.update { it.copy(hint = displayOf(puzzle, hint)) }
        return true
    }

    fun dismissHint() {
        _state.update { it.copy(hint = null) }
    }

    /**
     * Hardcore. With N animals placed, compares them with the solution. Solved stops the
     * clock and shows the win screen; Incorrect opens the dialog. A failed check costs nothing.
     */
    fun checkSolution(): CheckResult {
        val state = _state.value
        val puzzle = state.puzzle ?: return CheckResult.Incorrect(emptySet())
        val animals = state.board.animals
        val wrongCells = animals - puzzle.solutionCells
        if (wrongCells.isEmpty() && animals.size == puzzle.size) {
            _state.update { it.copy(failedCheck = null, errorCells = emptySet()) }
            gameSession.complete()
            return CheckResult.Solved
        }
        _state.update { it.copy(failedCheck = FailedCheck(wrongCells)) }
        return CheckResult.Incorrect(wrongCells)
    }

    fun dismissFailedCheck() {
        _state.update { it.copy(failedCheck = null) }
    }

    fun showWrongCount() {
        _state.update { state -> state.copy(failedCheck = state.failedCheck?.copy(isCountShown = true)) }
    }

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

    // ── Board changes ───────────────────────────────────────────────────────

    private fun applyEdit(edit: BoardEdit?) {
        if (edit == null) return
        val board = _state.value.board
        val edited = edit.changes.entries.fold(board) { current, (cell, mark) -> current.withMark(cell, mark) }
        applyChange(edited, recordUndo = !edit.continuesPrevious)
    }

    /**
     * The one path for every board change: undo, save, Classic conflicts and win,
     * clearing the hint highlight and, when one of them changed, the revealed errors.
     */
    private fun applyChange(newBoard: AnimalsBoard, recordUndo: Boolean) {
        val state = _state.value
        val puzzle = state.puzzle ?: return
        val oldBoard = state.board
        if (newBoard == oldBoard) return
        if (recordUndo) undoStack.push(oldBoard)

        AnimalsGameSaver.saveBoard(savedStateHandle, newBoard)
        AnimalsGameSaver.saveUndo(savedStateHandle, undoStack.toList())

        val changedCells = oldBoard.marks.indices.filterTo(HashSet()) { oldBoard[it] != newBoard[it] }
        _state.update {
            it.copy(
                board = newBoard,
                canUndo = undoStack.canUndo,
                conflictCells = conflictsOf(puzzle, newBoard),
                hint = null,
                errorCells = if (changedCells.any { cell -> cell in it.errorCells }) emptySet() else it.errorCells
            )
        }
        if (isClassic && AnimalsValidator.isSolved(puzzle, newBoard)) gameSession.complete()
    }

    private fun conflictsOf(puzzle: AnimalsPuzzle, board: AnimalsBoard): Set<Int> =
        if (!isClassic) {
            emptySet()
        } else {
            AnimalsValidator.conflicts(puzzle, board).flatMapTo(HashSet()) { listOf(it.first, it.second) }
        }

    private fun displayOf(puzzle: AnimalsPuzzle, hint: AnimalsHint): HintDisplay = when (hint) {
        is AnimalsHint.WrongMark -> HintDisplay(hint, setOf(hint.cell), emptySet())
        is AnimalsHint.Reveal -> HintDisplay(hint, setOf(hint.cell), emptySet())
        is AnimalsHint.Deduction -> HintDisplay(
            hint = hint,
            cells = hint.step.placements + hint.step.eliminations,
            areaCells = (hint.step.sources + hint.step.targets).flatMapTo(HashSet()) { area ->
                puzzle.cellsOf(area)
            }
        )
        AnimalsHint.Solved -> HintDisplay(hint, emptySet(), emptySet())
    }

    private companion object {
        /** Each entry is a whole board (≤ 81 marks), so the saved history stays small. */
        const val UNDO_LIMIT = 100
    }
}
