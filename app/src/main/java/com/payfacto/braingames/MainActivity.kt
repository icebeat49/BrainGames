package com.payfacto.braingames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.model.AppPreferences
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.core.theme.BrainGamesTheme
import com.payfacto.braingames.core.theme.DarkMode
import com.payfacto.braingames.core.ui.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var soundManager: SoundManager
    @Inject lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreLocale()
        enableEdgeToEdge()
        setContent {
            val prefs by preferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = AppPreferences()
            )

            soundManager.isMuted = prefs.isMuted

            val darkTheme = when (prefs.darkMode) {
                DarkMode.LIGHT  -> false
                DarkMode.DARK   -> true
                DarkMode.SYSTEM -> isSystemInDarkTheme()
            }

            BrainGamesTheme(appColorTheme = prefs.colorTheme, darkTheme = darkTheme) {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    soundManager = soundManager
                )
            }
        }
    }

    private fun restoreLocale() {
        val tag = runBlocking { preferencesRepository.preferences.first().languageTag }
        if (tag.isNotEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }
}
