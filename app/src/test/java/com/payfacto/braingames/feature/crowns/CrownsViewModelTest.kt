package com.payfacto.braingames.feature.crowns

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.model.CheckResult
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.data.HowToPlayPreferences
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.motion.MotionSettings
import com.payfacto.braingames.core.session.GameSession
import com.payfacto.braingames.core.sound.SoundEvent
import com.payfacto.braingames.core.ui.grid.GridPoint
import com.payfacto.braingames.core.ui.navigation.Screen
import com.payfacto.braingames.feature.crowns.CrownsTestGrids.uniqueFiveByFive
import com.payfacto.braingames.feature.crowns.data.CrownsPuzzleSource
import com.payfacto.braingames.feature.crowns.engine.CrownsHint
import com.payfacto.braingames.feature.crowns.engine.Technique
import com.payfacto.braingames.feature.crowns.input.TapEvent
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import com.payfacto.braingames.feature.crowns.model.CellMark
import com.payfacto.braingames.testutil.FakeGameSaves
import com.payfacto.braingames.testutil.MainDispatcherRule
import com.payfacto.braingames.testutil.FakePreferencesDataStore
import com.payfacto.braingames.testutil.fakePreferencesRepository
import com.payfacto.braingames.testutil.fakeStatsStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class CrownsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val puzzle = uniqueFiveByFive
    private fun cell(row: Int, col: Int) = puzzle.cellAt(row, col)
    private val emptyBoard = CrownsBoard.empty(5)

    /** Dependencies for one game; calling newViewModel again simulates recreation. */
    private inner class Fixture(mode: GameMode, animations: Boolean = false, howToPlaySeen: Boolean = true) {
        val savedState = SavedStateHandle(
            mapOf(Screen.ARG_DIFFICULTY to Difficulty.VERY_EASY.name, Screen.ARG_MODE to mode.name)
        )
        val source = RecordingSource(puzzle)
        private val preferences = fakePreferencesRepository()
        private val motion = MotionSettings { animations }
        val saves = FakeGameSaves()
        val stats = fakeStatsStore()
        val howToPlay = HowToPlayPreferences(FakePreferencesDataStore()).apply {
            if (howToPlaySeen) runBlocking { markSeen(GameType.CROWNS) }
        }

        private val routeMode = mode

        fun TestScope.newViewModel() =
            CrownsViewModel(savedState, source, preferences, testScheduler.timeSource, motion, howToPlay, saves, stats)
                .also { advanceUntilIdle() }

        /** The game opened with Continue after the app was closed: a fresh SavedStateHandle, the same saves. */
        fun TestScope.continueGame(): CrownsViewModel {
            val handle = SavedStateHandle(
                mapOf(
                    Screen.ARG_DIFFICULTY to Difficulty.VERY_EASY.name,
                    Screen.ARG_MODE to routeMode.name,
                    Screen.ARG_RESUME to true
                )
            )
            return CrownsViewModel(handle, source, preferences, testScheduler.timeSource, motion, howToPlay, saves, stats)
                .also { advanceUntilIdle() }
        }
    }

    private var clock = 10_000L

    private fun CrownsViewModel.tap(cell: Int, gapMillis: Long = 1_000) {
        val down = clock + gapMillis
        clock = down + 40
        val point = GridPoint(cell / puzzle.size + 0.5f, cell % puzzle.size + 0.5f)
        onTap(TapEvent(point, down, clock, DOUBLE_TAP_TIMEOUT, snapRadiusCells = 0.2f))
    }

    private fun CrownsViewModel.placeCrown(cell: Int) {
        tap(cell)
        tap(cell, gapMillis = 80)
    }

    private fun CrownsViewModel.placeAll(cells: Collection<Int>) = cells.forEach { placeCrown(it) }

    private fun TestScope.classic() = with(Fixture(GameMode.CLASSIC)) { newViewModel() }
    private fun TestScope.hardcore() = with(Fixture(GameMode.HARDCORE)) { newViewModel() }

    // ── Setup ───────────────────────────────────────────────────────────────

    @Test
    fun `reads difficulty and mode from the route and loads one puzzle`() = runTest(mainDispatcher.dispatcher) {
        val fixture = Fixture(GameMode.HARDCORE)
        val viewModel = with(fixture) { newViewModel() }

        assertEquals(Difficulty.VERY_EASY, viewModel.state.value.difficulty)
        assertEquals(GameMode.HARDCORE, viewModel.state.value.mode)
        assertEquals(listOf(Difficulty.VERY_EASY), fixture.source.requests)
        assertEquals(emptyBoard, viewModel.state.value.board)
    }

    @Test
    fun `taps reach the board through the gesture rules`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.tap(cell(2, 2))
        viewModel.placeCrown(cell(0, 0))

        assertEquals(CellMark.CROSS, viewModel.state.value.board[cell(2, 2)])
        assertEquals(CellMark.CROWN, viewModel.state.value.board[cell(0, 0)])
    }

    @Test
    fun `the question toggle changes what a tap writes`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.toggleQuestionMode()
        viewModel.tap(cell(2, 2))

        assertEquals(CellMark.QUESTION, viewModel.state.value.board[cell(2, 2)])
    }

    @Test
    fun `placing a crown never crosses out its neighbours`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeCrown(cell(2, 2))

        assertEquals(setOf(cell(2, 2)), viewModel.state.value.board.cellsWith(CellMark.CROWN))
        assertEquals(emptySet<Int>(), viewModel.state.value.board.cellsWith(CellMark.CROSS))
    }

    // ── Classic ─────────────────────────────────────────────────────────────

    @Test
    fun `classic marks crowns that break a rule`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeCrown(cell(0, 1))
        viewModel.placeCrown(cell(0, 2))

        assertEquals(setOf(cell(0, 1), cell(0, 2)), viewModel.state.value.conflictCells)
    }

    @Test
    fun `classic conflict marks go away once the conflict is fixed`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeCrown(cell(0, 1))
        viewModel.placeCrown(cell(0, 2))

        viewModel.tap(cell(0, 2))

        assertEquals(emptySet<Int>(), viewModel.state.value.conflictCells)
    }

    @Test
    fun `classic wins automatically when the grid is solved`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeAll(puzzle.solutionCells)

        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `a classic hint is applied, shown and spends one hint`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        val used = viewModel.useHint()

        val state = viewModel.state.value
        assertTrue(used)
        assertEquals(CellMark.CROWN, state.board[cell(0, 0)])
        val shown = state.hint!!.hint as CrownsHint.Deduction
        assertEquals(Technique.SINGLE_CANDIDATE, shown.step.technique)
        assertEquals(setOf(cell(0, 0)), state.hint!!.cells)
        assertEquals(setOf(cell(0, 0)), state.hint!!.areaCells) // region A is one cell
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    // ── Classic: wrong crowns ───────────────────────────────────────────────
    // Solution "02413": (4,4), (0,1), (0,2), (0,3), (0,4) and (1,0) are all wrong crowns.

    @Test
    fun `a wrong crown is shown at once and uses one hint`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeCrown(cell(4, 4))

        val state = viewModel.state.value
        assertEquals(CellMark.CROWN, state.board[cell(4, 4)])
        assertEquals(setOf(cell(4, 4)), state.wrongCrowns)
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `a shown wrong crown explains the hint it used`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeCrown(cell(4, 4))

        assertEquals(CrownsHint.WrongMark(cell(4, 4), CellMark.CROWN), viewModel.state.value.hint!!.hint)
    }

    @Test
    fun `a right crown uses no hint`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeCrown(cell(0, 0))

        assertEquals(emptySet<Int>(), viewModel.state.value.wrongCrowns)
        assertEquals(GameSession.DEFAULT_HINTS, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `a wrong crown placed again on the same cell uses no second hint`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.setMark(cell(4, 4), CellMark.CROWN)
        viewModel.setMark(cell(4, 4), CellMark.EMPTY)

        viewModel.setMark(cell(4, 4), CellMark.CROWN)

        assertEquals(setOf(cell(4, 4)), viewModel.state.value.wrongCrowns)
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `undo bringing back a wrong crown uses no second hint`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.setMark(cell(4, 4), CellMark.CROWN)
        viewModel.setMark(cell(4, 4), CellMark.EMPTY)

        viewModel.undo()

        assertEquals(setOf(cell(4, 4)), viewModel.state.value.wrongCrowns)
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `a removed wrong crown is no longer shown`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.setMark(cell(4, 4), CellMark.CROWN)

        viewModel.setMark(cell(4, 4), CellMark.EMPTY)

        assertEquals(emptySet<Int>(), viewModel.state.value.wrongCrowns)
    }

    @Test
    fun `with no hints left a wrong crown is not shown`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        val wrong = listOf(cell(4, 4), cell(0, 1), cell(0, 2), cell(0, 3), cell(0, 4), cell(1, 0))

        wrong.forEach { viewModel.setMark(it, CellMark.CROWN) }

        assertEquals(wrong.take(GameSession.DEFAULT_HINTS).toSet(), viewModel.state.value.wrongCrowns)
        assertEquals(0, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `a hint leaves a crown already shown as wrong to the player`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeCrown(cell(4, 4))

        viewModel.useHint()

        val state = viewModel.state.value
        assertEquals(CellMark.CROWN, state.board[cell(4, 4)])
        assertTrue(state.hint!!.hint is CrownsHint.Deduction)
        assertEquals(GameSession.DEFAULT_HINTS - 2, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `shown wrong crowns survive recreation without another hint`() = runTest(mainDispatcher.dispatcher) {
        val fixture = Fixture(GameMode.CLASSIC)
        with(fixture) { newViewModel() }.placeCrown(cell(4, 4))

        val recreated = with(fixture) { newViewModel() }
        recreated.setMark(cell(4, 4), CellMark.EMPTY)
        recreated.setMark(cell(4, 4), CellMark.CROWN)

        assertEquals(setOf(cell(4, 4)), recreated.state.value.wrongCrowns)
        assertEquals(GameSession.DEFAULT_HINTS - 1, recreated.session.value.hintsRemaining)
    }

    @Test
    fun `a new puzzle forgets the shown wrong crowns`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeCrown(cell(4, 4))

        viewModel.newPuzzle()
        advanceUntilIdle()
        viewModel.placeCrown(cell(4, 4))

        assertEquals(setOf(cell(4, 4)), viewModel.state.value.wrongCrowns)
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `hardcore never shows wrong crowns nor uses hints`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()

        viewModel.placeCrown(cell(4, 4))

        assertEquals(emptySet<Int>(), viewModel.state.value.wrongCrowns)
        assertNull(viewModel.state.value.hint)
    }

    // ── Crown counter ───────────────────────────────────────────────────────

    @Test
    fun `the counter counts every crown on the board, right or wrong`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()

        viewModel.placeCrown(cell(0, 0))
        viewModel.placeCrown(cell(4, 4))

        assertEquals(2, viewModel.state.value.crownCount)
    }

    @Test
    fun `a hint is one undo step and the hint stays spent`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.useHint()

        viewModel.undo()

        assertEquals(emptyBoard, viewModel.state.value.board)
        assertEquals(GameSession.DEFAULT_HINTS - 1, viewModel.session.value.hintsRemaining)
    }

    @Test
    fun `the hint highlight goes away with the next move`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.useHint()

        viewModel.tap(cell(4, 4))

        assertNull(viewModel.state.value.hint)
    }

    @Test
    fun `following hints alone solves the puzzle and wins`() = runTest(mainDispatcher.dispatcher) {
        val fixture = Fixture(GameMode.CLASSIC)
        val viewModel = with(fixture) { newViewModel() }

        repeat(GameSession.DEFAULT_HINTS) { viewModel.useHint() }
        // Five hints may not finish a 5x5; place what is left from the solution.
        viewModel.placeAll(puzzle.solutionCells.filter { viewModel.state.value.board[it] != CellMark.CROWN })

        assertTrue(viewModel.session.value.showWin)
    }

    // ── Hardcore ────────────────────────────────────────────────────────────

    @Test
    fun `hardcore shows no conflicts`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()

        viewModel.placeCrown(cell(0, 1))
        viewModel.placeCrown(cell(0, 2))

        assertEquals(emptySet<Int>(), viewModel.state.value.conflictCells)
    }

    @Test
    fun `hardcore has no hints`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()

        assertFalse(viewModel.useHint())
        assertEquals(0, viewModel.session.value.hintsRemaining)
        assertEquals(emptyBoard, viewModel.state.value.board)
    }

    @Test
    fun `a win locks the board, highlights it, then shows the win screen`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = with(Fixture(GameMode.CLASSIC, animations = true)) { newViewModel() }

        viewModel.placeAll(puzzle.solutionCells)
        val showWinDuringHighlight = viewModel.session.value.showWin
        viewModel.tap(cell(1, 1))
        val boardAfterLockedTap = viewModel.state.value.board
        advanceTimeBy(CrownsViewModel.WIN_HIGHLIGHT + 1.milliseconds)

        assertTrue(viewModel.state.value.isSolved)
        assertFalse(showWinDuringHighlight)
        assertEquals(CellMark.EMPTY, boardAfterLockedTap[cell(1, 1)])
        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `with animations off the win screen shows at once`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.placeAll(puzzle.solutionCells)

        assertTrue(viewModel.state.value.isSolved)
        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `a new puzzle unlocks the board`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeAll(puzzle.solutionCells)

        viewModel.newPuzzle()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSolved)
        assertTrue(viewModel.state.value.isPlayable)
    }

    @Test
    fun `hardcore does not win automatically`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()

        viewModel.placeAll(puzzle.solutionCells)

        assertFalse(viewModel.session.value.showWin)
    }

    @Test
    fun `check is enabled only with N crowns on the board`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells.take(4))
        val withFour = viewModel.state.value.canCheck

        viewModel.placeCrown(puzzle.solutionCells.last())

        assertFalse(withFour)
        assertTrue(viewModel.state.value.canCheck)
    }

    @Test
    fun `check on the solution returns Solved and shows the win`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells)

        assertEquals(CheckResult.Solved, viewModel.checkSolution())
        assertTrue(viewModel.session.value.showWin)
    }

    @Test
    fun `check returns Incorrect with exactly the misplaced crowns`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells - cell(0, 0) + cell(4, 4))

        assertEquals(CheckResult.Incorrect(setOf(cell(4, 4))), viewModel.checkSolution())
        assertEquals(setOf(cell(4, 4)), viewModel.state.value.failedCheck?.wrongCells)
    }

    @Test
    fun `show where marks the wrong crowns until one of them changes`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells - cell(0, 0) + cell(4, 4))
        viewModel.checkSolution()
        viewModel.showWrongCells()

        viewModel.tap(cell(2, 0))  // an unrelated cell: marks stay
        val afterOtherCell = viewModel.state.value.errorCells
        viewModel.tap(cell(4, 4))  // the marked cell: marks go
        val afterMarkedCell = viewModel.state.value.errorCells

        assertEquals(setOf(cell(4, 4)), afterOtherCell)
        assertEquals(emptySet<Int>(), afterMarkedCell)
    }

    @Test
    fun `show how many keeps the dialog open with the count`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        viewModel.placeAll(puzzle.solutionCells - cell(0, 0) + cell(4, 4))
        viewModel.checkSolution()

        viewModel.showWrongCount()

        assertEquals(1, viewModel.state.value.failedCheck?.wrongCount)
        assertTrue(viewModel.state.value.failedCheck!!.isCountShown)
    }

    // ── Undo ────────────────────────────────────────────────────────────────

    @Test
    fun `a double tap is one undo step`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeCrown(cell(2, 2))

        viewModel.undo()

        assertEquals(emptyBoard, viewModel.state.value.board)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `a drag is one undo step`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.onDragStart(GridPoint(4.5f, 0.5f))
        viewModel.onDragMove(GridPoint(4.5f, 2.5f))
        viewModel.onDragMove(GridPoint(4.5f, 4.5f))
        viewModel.onDragEnd()
        val crossed = viewModel.state.value.board.cellsWith(CellMark.CROSS)

        viewModel.undo()

        assertEquals((20..24).toSet(), crossed)
        assertEquals(emptyBoard, viewModel.state.value.board)
    }

    @Test
    fun `an erase drag clears marks, keeps the crown and is one undo step`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        (20..23).forEach { viewModel.tap(it) }         // X on (4,0)..(4,3)
        viewModel.placeCrown(cell(4, 4))
        val beforeErase = viewModel.state.value.board

        viewModel.onDragStart(GridPoint(4.5f, 0.5f))
        viewModel.onDragMove(GridPoint(4.5f, 4.5f))
        viewModel.onDragEnd()
        val erased = viewModel.state.value.board
        viewModel.undo()

        assertEquals(emptySet<Int>(), erased.cellsWith(CellMark.CROSS))
        assertEquals(CellMark.CROWN, erased[cell(4, 4)])
        assertEquals(beforeErase, viewModel.state.value.board)
    }

    @Test
    fun `clear marks removes every cross and question mark and keeps the crowns`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.tap(cell(1, 1))
        viewModel.toggleQuestionMode()
        viewModel.tap(cell(3, 3))
        viewModel.placeCrown(cell(0, 0))

        viewModel.clearMarks()

        val board = viewModel.state.value.board
        assertEquals(emptyBoard.withMark(cell(0, 0), CellMark.CROWN), board)
        assertFalse(viewModel.state.value.hasMarks)
    }

    @Test
    fun `clear marks is one undo step`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.tap(cell(1, 1))
        viewModel.tap(cell(2, 3))
        val beforeClear = viewModel.state.value.board

        viewModel.clearMarks()
        viewModel.undo()

        assertEquals(beforeClear, viewModel.state.value.board)
    }

    @Test
    fun `clear marks is unavailable without marks`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        val onEmptyBoard = viewModel.state.value.hasMarks

        viewModel.placeCrown(cell(0, 0))

        assertFalse(onEmptyBoard)
        assertFalse(viewModel.state.value.hasMarks)
    }

    @Test
    fun `clear marks with no marks changes nothing and adds no undo`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()

        viewModel.clearMarks()

        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `clear all empties the board and can be undone`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.tap(cell(1, 1))
        viewModel.placeCrown(cell(0, 0))
        val beforeClear = viewModel.state.value.board

        viewModel.clearAll()
        val cleared = viewModel.state.value.board
        viewModel.undo()

        assertEquals(emptyBoard, cleared)
        assertEquals(beforeClear, viewModel.state.value.board)
    }

    @Test
    fun `undo history is empty after a new puzzle`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.tap(cell(1, 1))

        viewModel.newPuzzle()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canUndo)
        assertEquals(emptyBoard, viewModel.state.value.board)
    }

    // ── How to play ─────────────────────────────────────────────────────────

    @Test
    fun `how to play opens by itself the first time only`() = runTest(mainDispatcher.dispatcher) {
        val fixture = Fixture(GameMode.CLASSIC, howToPlaySeen = false)
        val first = with(fixture) { newViewModel() }
        val openedFirstTime = first.state.value.showHowToPlay

        first.closeHowToPlay()
        advanceUntilIdle()
        val second = with(fixture) { newViewModel() }

        assertTrue(openedFirstTime)
        assertFalse(first.state.value.showHowToPlay)
        assertFalse(second.state.value.showHowToPlay)
    }

    @Test
    fun `how to play stays closed once seen and opens from the help button`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        val openedAtStart = viewModel.state.value.showHowToPlay

        viewModel.openHowToPlay()

        assertFalse(openedAtStart)
        assertTrue(viewModel.state.value.showHowToPlay)
    }

    @Test
    fun `the clock pauses while how to play is open`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.onScreenVisible()
        advanceTimeBy(3_000)
        viewModel.openHowToPlay()
        advanceTimeBy(10_000)
        val whileOpen = viewModel.session.value.elapsedSeconds

        viewModel.closeHowToPlay()
        advanceTimeBy(2_000)
        viewModel.onScreenHidden()

        assertEquals(3L, whileOpen)
        assertEquals(5L, viewModel.session.value.elapsedSeconds)
    }

    // ── Sounds ──────────────────────────────────────────────────────────────

    /** Sounds played from now on; sounds sent before (buffered) are dropped. */
    private fun TestScope.soundsOf(viewModel: CrownsViewModel): MutableList<SoundEvent> {
        val sounds = mutableListOf<SoundEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sounds.collect { sounds += it } }
        sounds.clear()
        return sounds
    }

    @Test
    fun `a mark taps and a crown has its own sound`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        val sounds = soundsOf(viewModel)

        viewModel.placeCrown(cell(0, 0))

        assertEquals(listOf(SoundEvent.TAP, SoundEvent.CROWN_PLACED), sounds)
    }

    @Test
    fun `a drag plays one sound, not one per cell`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        val sounds = soundsOf(viewModel)

        viewModel.onDragStart(GridPoint(4.5f, 0.5f))
        viewModel.onDragMove(GridPoint(4.5f, 4.5f))
        viewModel.onDragEnd()

        assertEquals(listOf(SoundEvent.TAP), sounds)
    }

    @Test
    fun `classic plays the conflict sound for a crown that breaks a rule`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.placeCrown(cell(0, 1))
        val sounds = soundsOf(viewModel)

        viewModel.placeCrown(cell(0, 2))

        assertEquals(SoundEvent.CONFLICT, sounds.last())
    }

    @Test
    fun `hardcore never plays a sound that reveals right or wrong`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        val sounds = soundsOf(viewModel)

        viewModel.placeCrown(cell(0, 1))
        viewModel.placeCrown(cell(0, 2))
        val wrongCrown = puzzle.cellCount - 1
        viewModel.placeCrown(wrongCrown)

        assertEquals(
            listOf(
                SoundEvent.TAP, SoundEvent.CROWN_PLACED,
                SoundEvent.TAP, SoundEvent.CROWN_PLACED,
                SoundEvent.TAP, SoundEvent.CROWN_PLACED
            ),
            sounds
        )
    }

    @Test
    fun `a failed check plays its sound`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = hardcore()
        val sounds = soundsOf(viewModel)

        viewModel.checkSolution()

        assertEquals(listOf(SoundEvent.CHECK_FAILED), sounds)
    }

    @Test
    fun `a hint plays the hint sound`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        val sounds = soundsOf(viewModel)

        viewModel.useHint()

        assertEquals(listOf(SoundEvent.HINT), sounds)
    }

    @Test
    fun `the win sound plays once, when the grid is solved`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        val sounds = soundsOf(viewModel)

        viewModel.placeAll(puzzle.solutionCells)

        assertEquals(1, sounds.count { it == SoundEvent.WIN })
        assertEquals(SoundEvent.WIN, sounds.last())
    }

    @Test
    fun `undo and the clear buttons tap`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = classic()
        viewModel.tap(cell(2, 2))
        viewModel.placeCrown(cell(0, 0))
        val sounds = soundsOf(viewModel)

        viewModel.undo()
        val afterUndo = sounds.toList()
        viewModel.placeCrown(cell(0, 0))
        sounds.clear()
        viewModel.clearMarks()
        viewModel.clearAll()

        assertEquals(listOf(SoundEvent.TAP), afterUndo)
        assertEquals(listOf(SoundEvent.TAP, SoundEvent.TAP), sounds)
    }

    // ── Saved state ─────────────────────────────────────────────────────────

    @Test
    fun `a recreated ViewModel restores puzzle, marks, undo, toggle and mode without regenerating`() =
        runTest(mainDispatcher.dispatcher) {
            val fixture = Fixture(GameMode.HARDCORE)
            val original = with(fixture) { newViewModel() }
            original.tap(cell(1, 1))
            original.placeCrown(cell(0, 0))
            original.toggleQuestionMode()

            val recreated = with(fixture) { newViewModel() }
            val restored = recreated.state.value
            recreated.undo()
            recreated.undo()

            assertEquals(1, fixture.source.requests.size)
            assertEquals(puzzle, restored.puzzle)
            assertEquals(original.state.value.board, restored.board)
            assertTrue(restored.isQuestionMode)
            assertEquals(GameMode.HARDCORE, restored.mode)
            assertEquals(emptyBoard, recreated.state.value.board)
        }

    @Test
    fun `continue after the app was closed restores the game and its shown wrong crowns`() =
        runTest(mainDispatcher.dispatcher) {
            val fixture = Fixture(GameMode.CLASSIC)
            val original = with(fixture) { newViewModel() }
            original.tap(cell(1, 1))
            original.placeCrown(cell(4, 4))
            original.toggleQuestionMode()
            original.onScreenHidden()

            val resumed = with(fixture) { continueGame() }
            val restored = resumed.state.value

            assertEquals(1, fixture.source.requests.size)
            assertEquals(puzzle, restored.puzzle)
            assertEquals(original.state.value.board, restored.board)
            assertTrue(restored.isQuestionMode)
            assertTrue(restored.canUndo)
            assertEquals(setOf(cell(4, 4)), restored.wrongCrowns)
            assertEquals(GameSession.DEFAULT_HINTS - 1, resumed.session.value.hintsRemaining)
        }

    @Test
    fun `a won game is no longer saved`() = runTest(mainDispatcher.dispatcher) {
        val fixture = Fixture(GameMode.CLASSIC)
        val viewModel = with(fixture) { newViewModel() }

        puzzle.solutionCells.forEach { viewModel.placeCrown(it) }
        advanceUntilIdle()
        viewModel.onScreenHidden()

        assertEquals(null, fixture.saves[GameType.CROWNS])
    }

    private class RecordingSource(private val puzzle: CrownsPuzzle) : CrownsPuzzleSource {
        val requests = mutableListOf<Difficulty>()

        override suspend fun newPuzzle(difficulty: Difficulty): CrownsPuzzle {
            requests += difficulty
            return puzzle
        }
    }

    private companion object {
        const val DOUBLE_TAP_TIMEOUT = 300L
    }
}
