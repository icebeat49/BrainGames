package com.payfacto.braingames.testutil

import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.save.GameSave
import com.payfacto.braingames.core.save.GameSaves
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Saves kept in memory: what a game wrote is visible at once. */
class FakeGameSaves : GameSaves {
    private val saves = MutableStateFlow<Map<GameType, GameSave>>(emptyMap())

    /** The current save of [game], for assertions. */
    operator fun get(game: GameType): GameSave? = saves.value[game]

    override fun saved(game: GameType): Flow<GameSave?> = saves.map { it[game] }

    override fun save(game: GameType, save: GameSave) {
        saves.value += game to save
    }

    override fun clear(game: GameType) {
        saves.value -= game
    }
}
