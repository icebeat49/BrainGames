package com.payfacto.braingames.feature.sudoku.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.payfacto.braingames.feature.sudoku.model.SudokuMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Sudoku-only settings, kept in the app's DataStore so core preferences stay game-agnostic. */
@Singleton
class SudokuPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val lastMode: Flow<SudokuMode> = dataStore.data.map { SudokuMode.fromName(it[KEY_LAST_MODE]) }

    suspend fun setLastMode(mode: SudokuMode) {
        dataStore.edit { it[KEY_LAST_MODE] = mode.name }
    }

    private companion object {
        val KEY_LAST_MODE = stringPreferencesKey("sudoku_last_mode")
    }
}
