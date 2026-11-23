package com.payfacto.braingames.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun BrainGamesTheme(
    appColorTheme: AppColorTheme = AppColorTheme.OCEAN,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = appColorScheme(appColorTheme, darkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BrainGamesTypography,
        content = content
    )
}
