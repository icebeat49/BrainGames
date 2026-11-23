package com.payfacto.braingames.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.payfacto.braingames.core.model.AppPreferences
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.theme.AppColorTheme
import com.payfacto.braingames.core.theme.DarkMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val COLOR_THEME       = stringPreferencesKey("color_theme")
        val DARK_MODE         = stringPreferencesKey("dark_mode")
        val LANGUAGE_TAG      = stringPreferencesKey("language_tag")
        val IS_MUTED          = booleanPreferencesKey("is_muted")
        val SUDOKU_DIFFICULTY = stringPreferencesKey("sudoku_difficulty")
        val KAKURO_DIFFICULTY = stringPreferencesKey("kakuro_difficulty")
        val WS_DIFFICULTY     = stringPreferencesKey("ws_difficulty")
    }

    val preferences: Flow<AppPreferences> = dataStore.data.map { prefs ->
        AppPreferences(
            colorTheme = prefs[Keys.COLOR_THEME]
                ?.let { runCatching { AppColorTheme.valueOf(it) }.getOrNull() }
                ?: AppColorTheme.OCEAN,
            darkMode = prefs[Keys.DARK_MODE]
                ?.let { runCatching { DarkMode.valueOf(it) }.getOrNull() }
                ?: DarkMode.SYSTEM,
            languageTag = prefs[Keys.LANGUAGE_TAG] ?: "",
            isMuted = prefs[Keys.IS_MUTED] ?: false,
            sudokuDifficulty = prefs[Keys.SUDOKU_DIFFICULTY]
                ?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() }
                ?: Difficulty.MEDIUM,
            kakuroDifficulty = prefs[Keys.KAKURO_DIFFICULTY]
                ?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() }
                ?: Difficulty.MEDIUM,
            wordSearchDifficulty = prefs[Keys.WS_DIFFICULTY]
                ?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() }
                ?: Difficulty.MEDIUM,
        )
    }

    suspend fun setColorTheme(theme: AppColorTheme) {
        dataStore.edit { it[Keys.COLOR_THEME] = theme.name }
    }

    suspend fun setDarkMode(mode: DarkMode) {
        dataStore.edit { it[Keys.DARK_MODE] = mode.name }
    }

    suspend fun setLanguageTag(tag: String) {
        dataStore.edit { it[Keys.LANGUAGE_TAG] = tag }
    }

    suspend fun setMuted(muted: Boolean) {
        dataStore.edit { it[Keys.IS_MUTED] = muted }
    }

    suspend fun setSudokuDifficulty(difficulty: Difficulty) {
        dataStore.edit { it[Keys.SUDOKU_DIFFICULTY] = difficulty.name }
    }

    suspend fun setKakuroDifficulty(difficulty: Difficulty) {
        dataStore.edit { it[Keys.KAKURO_DIFFICULTY] = difficulty.name }
    }

    suspend fun setWordSearchDifficulty(difficulty: Difficulty) {
        dataStore.edit { it[Keys.WS_DIFFICULTY] = difficulty.name }
    }
}
