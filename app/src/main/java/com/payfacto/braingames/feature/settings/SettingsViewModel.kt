package com.payfacto.braingames.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.locale.AppLanguage
import com.payfacto.braingames.core.model.AppPreferences
import com.payfacto.braingames.core.theme.AppColorTheme
import com.payfacto.braingames.core.theme.DarkMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PreferencesRepository
) : ViewModel() {

    val preferences = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppPreferences()
    )

    fun setColorTheme(theme: AppColorTheme) {
        viewModelScope.launch { repository.setColorTheme(theme) }
    }

    fun setDarkMode(mode: DarkMode) {
        viewModelScope.launch { repository.setDarkMode(mode) }
    }

    private val _language = MutableStateFlow(AppLanguage.current())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    /** Applies at once: the platform or AppCompat stores the choice and recreates the screen. */
    fun setLanguage(language: AppLanguage) {
        _language.value = language
        AppLanguage.apply(language)
    }

    fun toggleMute() {
        viewModelScope.launch {
            repository.setMuted(!preferences.value.isMuted)
        }
    }
}
