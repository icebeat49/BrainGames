package com.payfacto.braingames.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Keeps the screen from dimming and turning off while this composable is shown. A puzzle can
 * take minutes of thinking without a touch; the system's screen timeout applies again once
 * the game screen is left.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
