package com.payfacto.braingames.core.ui.navigation

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.data.GameModePreferences
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.testutil.FakePreferencesDataStore
import com.payfacto.braingames.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DifficultyPickerViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val preferences = GameModePreferences(FakePreferencesDataStore())

    private fun TestScope.pickerFor(game: GameType) =
        DifficultyPickerViewModel(SavedStateHandle(mapOf(Screen.ARG_GAME_TYPE to game.name)), preferences)
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
}
