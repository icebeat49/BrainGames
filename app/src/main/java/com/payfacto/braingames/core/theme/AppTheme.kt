package com.payfacto.braingames.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun BrainGamesTheme(
    appColorTheme: AppColorTheme = AppColorTheme.OCEAN,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = appColorScheme(appColorTheme, darkTheme)
    CompositionLocalProvider(
        LocalRegionPalette provides if (darkTheme) DarkRegionPalette else LightRegionPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BrainGamesTypography,
            content = content
        )
    }
}
