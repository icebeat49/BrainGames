package com.payfacto.braingames.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.config.FeatureFlags
import com.payfacto.braingames.core.data.GameOrderPreferences
import com.payfacto.braingames.core.model.GameType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel(
    private val preferences: GameOrderPreferences,
    private val isShown: (GameType) -> Boolean
) : ViewModel() {

    @Inject
    constructor(preferences: GameOrderPreferences) : this(preferences, FeatureFlags::isEnabled)

    /** The games on Home, in the player's order. */
    val games: StateFlow<List<GameType>> = preferences.order
        .map { order -> order.filter(isShown) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameType.entries.filter(isShown))

    /**
     * Saves [shown] as the new order of the games on Home. Hidden games keep their place
     * among each other and follow the shown ones.
     */
    fun reorder(shown: List<GameType>) {
        viewModelScope.launch {
            val hidden = preferences.order.first().filterNot { it in shown }
            preferences.setOrder(shown + hidden)
        }
    }

    /** Moves [game] one place earlier (-1) or later (+1): the accessibility alternative to dragging. */
    fun move(game: GameType, by: Int) {
        val order = games.value.toMutableList()
        val from = order.indexOf(game)
        val to = from + by
        if (from < 0 || to !in order.indices) return
        order.add(to, order.removeAt(from))
        reorder(order)
    }
}
