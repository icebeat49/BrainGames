package com.payfacto.braingames.core.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.GameModePreferences
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the mode choice on the difficulty picker; remembers the last mode per game. */
@HiltViewModel
class DifficultyPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val preferences: GameModePreferences
) : ViewModel() {

    private val game: GameType = GameType.entries
        .firstOrNull { it.name == savedStateHandle.get<String>(Screen.ARG_GAME_TYPE) }
        ?: GameType.SUDOKU

    val mode: StateFlow<GameMode> = preferences.lastMode(game).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GameMode.CLASSIC
    )

    fun select(mode: GameMode) {
        viewModelScope.launch { preferences.setLastMode(game, mode) }
    }
}
