package com.payfacto.braingames.core.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.GameModePreferences
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.save.GameSave
import com.payfacto.braingames.core.save.GameSaves
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the difficulty picker: the mode choice (last one remembered per game) and the saved game, if any. */
@HiltViewModel
class DifficultyPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val preferences: GameModePreferences,
    saves: GameSaves
) : ViewModel() {

    private val game: GameType = GameType.entries
        .firstOrNull { it.name == savedStateHandle.get<String>(Screen.ARG_GAME_TYPE) }
        ?: GameType.SUDOKU

    val mode: StateFlow<GameMode> = preferences.lastMode(game).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GameMode.CLASSIC
    )

    /** The game in progress, offered as Continue. */
    val saved: StateFlow<GameSave?> = saves.saved(game).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null
    )

    fun select(mode: GameMode) {
        viewModelScope.launch { preferences.setLastMode(game, mode) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
