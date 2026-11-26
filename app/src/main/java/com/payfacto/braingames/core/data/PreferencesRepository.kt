package com.payfacto.braingames.core.data

import com.payfacto.braingames.core.model.AppPreferences
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.theme.AppColorTheme
import com.payfacto.braingames.core.theme.DarkMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataSource: PreferencesDataSource
) {
    val preferences: Flow<AppPreferences> = dataSource.preferences

    suspend fun setColorTheme(theme: AppColorTheme) = dataSource.setColorTheme(theme)
    suspend fun setDarkMode(mode: DarkMode) = dataSource.setDarkMode(mode)
    suspend fun takeLegacyLanguageTag(): String? = dataSource.takeLegacyLanguageTag()
    suspend fun setMuted(muted: Boolean) = dataSource.setMuted(muted)
    suspend fun setSudokuDifficulty(difficulty: Difficulty) = dataSource.setSudokuDifficulty(difficulty)
    suspend fun setKakuroDifficulty(difficulty: Difficulty) = dataSource.setKakuroDifficulty(difficulty)
    suspend fun setWordSearchDifficulty(difficulty: Difficulty) = dataSource.setWordSearchDifficulty(difficulty)
}
