package com.payfacto.braingames.feature.sudoku

import com.payfacto.braingames.feature.sudoku.data.SudokuPreferences
import com.payfacto.braingames.feature.sudoku.model.SudokuMode
import com.payfacto.braingames.testutil.FakePreferencesDataStore
import com.payfacto.braingames.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SudokuModeViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val preferences = SudokuPreferences(FakePreferencesDataStore())

    @Test
    fun `classic is preselected the first time`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = SudokuModeViewModel(preferences)
        advanceUntilIdle()

        assertEquals(SudokuMode.CLASSIC, viewModel.mode.value)
    }

    @Test
    fun `the last mode used is preselected next time`() = runTest(mainDispatcher.dispatcher) {
        SudokuModeViewModel(preferences).select(SudokuMode.HARDCORE)
        advanceUntilIdle()

        val nextVisit = SudokuModeViewModel(preferences)
        advanceUntilIdle()

        assertEquals(SudokuMode.HARDCORE, nextVisit.mode.value)
    }
}
