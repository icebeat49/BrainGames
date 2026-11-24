package com.payfacto.braingames.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.luminance

@Composable
fun BrainGamesTheme(
    appColorTheme: AppColorTheme = AppColorTheme.OCEAN,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = appColorScheme(appColorTheme, darkTheme)
    CompositionLocalProvider(LocalRegionPalette provides regionPaletteFor(colorScheme)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BrainGamesTypography,
            content = content
        )
    }
}

/**
 * Region colours follow the scheme actually in use, not the dark-mode setting: Midnight is
 * always dark, and light regions under its light marks (onSurface) would be unreadable.
 */
internal fun regionPaletteFor(colorScheme: ColorScheme): RegionPalette =
    if (colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE) DarkRegionPalette else LightRegionPalette

private const val DARK_SURFACE_LUMINANCE = 0.5f
