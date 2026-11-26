package com.payfacto.braingames.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfacto.braingames.R
import com.payfacto.braingames.core.config.FeatureFlags
import com.payfacto.braingames.core.locale.AppLanguage
import com.payfacto.braingames.core.theme.AppColorTheme
import com.payfacto.braingames.core.theme.DarkMode
import com.payfacto.braingames.core.theme.appColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Theme ────────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppColorTheme.entries.forEach { theme ->
                    ThemeSwatch(
                        theme = theme,
                        isSelected = theme == prefs.colorTheme,
                        onClick = { viewModel.setColorTheme(theme) }
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 20.dp))

            // ── Dark Mode ────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_dark_mode),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = prefs.darkMode == DarkMode.SYSTEM,
                    onClick = { viewModel.setDarkMode(DarkMode.SYSTEM) },
                    label = { Text(stringResource(R.string.settings_dark_mode_system)) }
                )
                FilterChip(
                    selected = prefs.darkMode == DarkMode.LIGHT,
                    onClick = { viewModel.setDarkMode(DarkMode.LIGHT) },
                    label = { Text(stringResource(R.string.settings_dark_mode_light)) }
                )
                FilterChip(
                    selected = prefs.darkMode == DarkMode.DARK,
                    onClick = { viewModel.setDarkMode(DarkMode.DARK) },
                    label = { Text(stringResource(R.string.settings_dark_mode_dark)) }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 20.dp))

            // ── Language ─────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = language == AppLanguage.ENGLISH,
                    onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                    label = { Text(stringResource(R.string.settings_lang_english)) }
                )
                FilterChip(
                    selected = language == AppLanguage.FRENCH,
                    onClick = { viewModel.setLanguage(AppLanguage.FRENCH) },
                    label = { Text(stringResource(R.string.settings_lang_french)) }
                )
                FilterChip(
                    selected = language == AppLanguage.SYSTEM,
                    onClick = { viewModel.setLanguage(AppLanguage.SYSTEM) },
                    label = { Text(stringResource(R.string.settings_lang_system)) }
                )
            }

            // ── Sound ─────────────────────────────────────────────────────────
            if (FeatureFlags.isSoundEnabled) {
                HorizontalDivider(Modifier.padding(vertical = 20.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_sound)) },
                    leadingContent = {
                        Icon(
                            if (prefs.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = !prefs.isMuted,
                            onCheckedChange = { viewModel.toggleMute() }
                        )
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: AppColorTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scheme = appColorScheme(theme, darkTheme = false)
    val label = stringResource(
        when (theme) {
            AppColorTheme.OCEAN    -> R.string.theme_ocean
            AppColorTheme.FOREST   -> R.string.theme_forest
            AppColorTheme.SUNSET   -> R.string.theme_sunset
            AppColorTheme.LAVENDER -> R.string.theme_lavender
            AppColorTheme.MIDNIGHT -> R.string.theme_midnight
        }
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(scheme.primary)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
