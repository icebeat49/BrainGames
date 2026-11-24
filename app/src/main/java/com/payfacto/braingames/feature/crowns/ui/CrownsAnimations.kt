package com.payfacto.braingames.feature.crowns.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.launch

/** Short, decorative board animations. All are skipped when system animations are off. */
object CrownsAnimations {
    const val CROWN_IN_MILLIS = 160
    const val CROWN_OUT_MILLIS = 120

    /** Rise + fall of the whole-board glow on a win; the win screen waits for it. */
    const val WIN_GLOW_IN_MILLIS = 250
    const val WIN_GLOW_OUT_MILLIS = 450
    const val WIN_HIGHLIGHT_MILLIS = WIN_GLOW_IN_MILLIS + WIN_GLOW_OUT_MILLIS

    /** A crown starts (or ends) at this fraction of its size. */
    const val CROWN_MIN_SCALE = 0.4f
}

/**
 * Per-cell crown progress, 0 (gone) to 1 (fully drawn), for crowns appearing or
 * disappearing right now. Cells missing from the map are drawn as they are.
 * Removed crowns stay in the map, fading, until their animation ends.
 *
 * Read the values while drawing: they change every frame, and reading them there only
 * redraws the canvas. A new [puzzle] starts over without animating.
 */
@Composable
internal fun rememberCrownTransitions(
    puzzle: Any,
    crowns: Set<Int>,
    enabled: Boolean
): Map<Int, Animatable<Float, AnimationVector1D>> {
    val transitions: SnapshotStateMap<Int, Animatable<Float, AnimationVector1D>> =
        remember(puzzle) { mutableStateMapOf() }
    val previous = remember(puzzle) { arrayOfNulls<Set<Int>>(1) }
    // Animations run in this scope, not the effect's, so a quick next change doesn't freeze them.
    val scope = rememberCoroutineScope()

    LaunchedEffect(puzzle, crowns, enabled) {
        val before = previous[0]
        previous[0] = crowns
        if (before == null || !enabled) {
            transitions.clear()
            return@LaunchedEffect
        }
        fun animate(cell: Int, from: Float, to: Float, millis: Int) {
            val animatable = Animatable(transitions[cell]?.value ?: from)
            transitions[cell] = animatable
            scope.launch {
                animatable.animateTo(to, tween(millis, easing = FastOutSlowInEasing))
                if (transitions[cell] === animatable) transitions.remove(cell)
            }
        }
        (crowns - before).forEach { animate(it, from = 0f, to = 1f, CrownsAnimations.CROWN_IN_MILLIS) }
        (before - crowns).forEach { animate(it, from = 1f, to = 0f, CrownsAnimations.CROWN_OUT_MILLIS) }
    }
    return transitions
}
