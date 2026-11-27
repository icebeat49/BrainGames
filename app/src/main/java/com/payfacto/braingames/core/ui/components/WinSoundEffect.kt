package com.payfacto.braingames.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.payfacto.braingames.core.sound.SoundEvent
import com.payfacto.braingames.core.sound.SoundManager

/**
 * Plays the win sound once when the win screen appears. The win screen survives rotation,
 * so whether the sound was already played is saved with it and it isn't played again.
 */
@Composable
fun WinSoundEffect(showWin: Boolean, soundManager: SoundManager) {
    var played by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(showWin) {
        when {
            !showWin -> played = false
            !played -> {
                soundManager.play(SoundEvent.WIN)
                played = true
            }
        }
    }
}
