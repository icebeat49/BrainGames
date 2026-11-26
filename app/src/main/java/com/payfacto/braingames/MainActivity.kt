package com.payfacto.braingames

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.locale.AppLanguage
import com.payfacto.braingames.core.model.AppPreferences
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.core.theme.BrainGamesTheme
import com.payfacto.braingames.core.theme.DarkMode
import com.payfacto.braingames.core.ui.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * An [AppCompatActivity] so that `AppCompatDelegate.setApplicationLocales` takes effect:
 * it does nothing unless an AppCompat activity exists.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var soundManager: SoundManager
    @Inject lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        migrateLegacyLanguage()
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

    /**
     * Older versions saved the language in DataStore without ever applying it. Apply that
     * choice once, unless a language is already set, then forget it.
     */
    private fun migrateLegacyLanguage() {
        lifecycleScope.launch {
            val legacy = preferencesRepository.takeLegacyLanguageTag() ?: return@launch
            if (AppLanguage.current() == AppLanguage.SYSTEM) {
                AppLanguage.apply(AppLanguage.fromTags(legacy))
            }
        }
    }
}
