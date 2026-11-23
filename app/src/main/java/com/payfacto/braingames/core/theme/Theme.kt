package com.payfacto.braingames.core.theme

import androidx.compose.material3.ColorScheme

enum class AppColorTheme {
    OCEAN,
    FOREST,
    SUNSET,
    LAVENDER,
    MIDNIGHT
}

enum class DarkMode {
    SYSTEM,
    LIGHT,
    DARK
}

fun appColorScheme(theme: AppColorTheme, darkTheme: Boolean): ColorScheme = when (theme) {
    AppColorTheme.OCEAN    -> if (darkTheme) oceanDarkScheme    else oceanLightScheme
    AppColorTheme.FOREST   -> if (darkTheme) forestDarkScheme   else forestLightScheme
    AppColorTheme.SUNSET   -> if (darkTheme) sunsetDarkScheme   else sunsetLightScheme
    AppColorTheme.LAVENDER -> if (darkTheme) lavenderDarkScheme else lavenderLightScheme
    AppColorTheme.MIDNIGHT -> midnightDarkScheme
}
