package com.payfacto.braingames.core.ui.navigation

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.data.GameModePreferences
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.save.GameSave
import com.payfacto.braingames.testutil.FakePreferencesDataStore
import com.payfacto.braingames.testutil.FakeGameSaves
import com.payfacto.braingames.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DifficultyPickerViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val preferences = GameModePreferences(FakePreferencesDataStore())
    private val saves = FakeGameSaves()

    private fun TestScope.pickerFor(game: GameType) =
        DifficultyPickerViewModel(SavedStateHandle(mapOf(Screen.ARG_GAME_TYPE to game.name)), preferences, saves)
            .also { advanceUntilIdle() }

    @Test
    fun `classic is preselected the first time`() = runTest(mainDispatcher.dispatcher) {
        assertEquals(GameMode.CLASSIC, pickerFor(GameType.SUDOKU).mode.value)
    }

    @Test
    fun `the last mode used is preselected next time`() = runTest(mainDispatcher.dispatcher) {
        pickerFor(GameType.SUDOKU).select(GameMode.HARDCORE)
        advanceUntilIdle()

        assertEquals(GameMode.HARDCORE, pickerFor(GameType.SUDOKU).mode.value)
    }

    @Test
    fun `each game remembers its own mode`() = runTest(mainDispatcher.dispatcher) {
        pickerFor(GameType.SUDOKU).select(GameMode.HARDCORE)
        advanceUntilIdle()

        assertEquals(GameMode.CLASSIC, pickerFor(GameType.KAKURO).mode.value)
    }

    @Test
    fun `the game in progress is offered as Continue`() = runTest(mainDispatcher.dispatcher) {
        val save = GameSave(Difficulty.HARD, GameMode.HARDCORE, emptyMap())
        saves.save(GameType.CROWNS, save)

        val picker = pickerFor(GameType.CROWNS)
        val collector = launch { picker.saved.collect {} }
        advanceUntilIdle()

        assertEquals(save, picker.saved.value)
        collector.cancel()
    }

    @Test
    fun `nothing is offered without a save`() = runTest(mainDispatcher.dispatcher) {
        val picker = pickerFor(GameType.CROWNS)
        val collector = launch { picker.saved.collect {} }
        advanceUntilIdle()

        assertNull(picker.saved.value)
        collector.cancel()
    }
}
