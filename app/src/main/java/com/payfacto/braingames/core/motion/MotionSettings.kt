package com.payfacto.braingames.core.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Whether decorative animations should play. */
fun interface MotionSettings {
    fun animationsEnabled(): Boolean
}

/**
 * Follows the system "Remove animations" accessibility setting, which sets the animator
 * duration scale to 0. Read on each call, so a change applies without restarting.
 */
class SystemMotionSettings @Inject constructor(
    @ApplicationContext private val context: Context
) : MotionSettings {
    override fun animationsEnabled(): Boolean = animationsEnabled(context)
}

fun animationsEnabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

/** [animationsEnabled] for composables; read once per composition of the caller. */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) { animationsEnabled(context) }
}
