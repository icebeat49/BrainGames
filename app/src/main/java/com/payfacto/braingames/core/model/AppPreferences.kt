package com.payfacto.braingames.core.model

import com.payfacto.braingames.core.theme.AppColorTheme
import com.payfacto.braingames.core.theme.DarkMode

data class AppPreferences(
    val colorTheme: AppColorTheme = AppColorTheme.OCEAN,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val languageTag: String = "",
    val isMuted: Boolean = false,
    val sudokuDifficulty: Difficulty = Difficulty.MEDIUM,
    val kakuroDifficulty: Difficulty = Difficulty.MEDIUM,
    val wordSearchDifficulty: Difficulty = Difficulty.MEDIUM
)
