package com.payfacto.braingames.feature.sudoku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.feature.sudoku.data.SudokuPreferences
import com.payfacto.braingames.feature.sudoku.model.SudokuMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the mode choice on Sudoku's difficulty picker; remembers the last mode used. */
@HiltViewModel
class SudokuModeViewModel @Inject constructor(
    private val preferences: SudokuPreferences
) : ViewModel() {

    val mode: StateFlow<SudokuMode> = preferences.lastMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SudokuMode.CLASSIC
    )

    fun select(mode: SudokuMode) {
        viewModelScope.launch { preferences.setLastMode(mode) }
    }
}
