package com.payfacto.braingames.feature.crowns.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.payfacto.braingames.feature.crowns.CrownsUiState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.motion.rememberAnimationsEnabled
import com.payfacto.braingames.core.sound.SoundEvent
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.core.ui.components.CheckFailedDialog
import com.payfacto.braingames.core.ui.components.GameTopBar
import com.payfacto.braingames.core.ui.components.WinAnimationOverlay
import com.payfacto.braingames.feature.crowns.CrownsViewModel

@Composable
fun CrownsScreen(
    soundManager: SoundManager,
    onBack: () -> Unit,
    viewModel: CrownsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isClassic = state.mode == GameMode.CLASSIC
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { viewModel.onScreenHidden() }
    }

    // The ViewModel picks every board sound (win included, played once as the board glows).
    LaunchedEffect(viewModel) {
        viewModel.sounds.collect(soundManager::play)
    }

    // Brief glow of the whole board on a win, before the win screen (which waits for it).
    val animate = rememberAnimationsEnabled()
    val winGlow = remember { Animatable(0f) }
    LaunchedEffect(state.isSolved) {
        if (state.isSolved && animate && !session.showWin) {
            winGlow.animateTo(1f, tween(CrownsAnimations.WIN_GLOW_IN_MILLIS))
            winGlow.animateTo(0f, tween(CrownsAnimations.WIN_GLOW_OUT_MILLIS))
        } else {
            winGlow.snapTo(0f)
        }
    }

    Scaffold(
        topBar = {
            GameTopBar(
                title = stringResource(R.string.home_play_crowns),
                hintsRemaining = session.hintsRemaining,
                isMuted = session.isMuted,
                onBack = onBack,
                onHint = if (isClassic) {
                    { viewModel.useHint() }
                } else null,
                onNewPuzzle = {
                    soundManager.play(SoundEvent.TAP)
                    viewModel.newPuzzle()
                },
                onToggleMute = viewModel::toggleMute,
                onHelp = viewModel::openHowToPlay
            )
        }
    ) { padding ->
      BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        // The grid keeps clear of the system back-gesture zones: a drag starting in the first
        // or last column would otherwise leave the game on phones with gesture navigation.
        val sideMargin = gridSideMargin()
        val board: @Composable (Dp) -> Unit = { side ->
            val puzzle = state.puzzle
            if (puzzle != null && puzzle.size == state.board.size) {
                CrownsGrid(
                    puzzle = puzzle,
                    board = state.board,
                    highlights = CrownsGridHighlights(
                        conflictCells = state.conflictCells,
                        errorCells = state.errorCells,
                        hintCells = state.hint?.cells.orEmpty(),
                        hintAreaCells = state.hint?.areaCells.orEmpty()
                    ),
                    isQuestionMode = state.isQuestionMode,
                    input = CrownsGridInput(
                        onTap = viewModel::onTap,
                        onDragStart = viewModel::onDragStart,
                        onDragMove = viewModel::onDragMove,
                        onDragEnd = viewModel::onDragEnd,
                        onSetMark = viewModel::setMark
                    ),
                    modifier = Modifier.size(side),
                    winGlow = { winGlow.value }
                )
            } else {
                Spacer(Modifier.size(side))
            }
        }
        val status: @Composable () -> Unit = {
            Text(
                text = if (isClassic) {
                    stringResource(R.string.hints_remaining, session.hintsRemaining)
                } else {
                    stringResource(R.string.mode_hardcore_label)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val panel: @Composable () -> Unit = {
            ControlsPanel(
                state = state,
                isClassic = isClassic,
                onDismissHint = viewModel::dismissHint,
                actions = ControlActions(
                    onUndo = viewModel::undo,
                    onToggleQuestionMode = viewModel::toggleQuestionMode,
                    onClearMarks = viewModel::clearMarks,
                    onClearAll = { confirmClear = true },
                    onCheck = { viewModel.checkSolution() }
                )
            )
        }

        if (maxWidth > maxHeight) {
            // Landscape: the grid takes the full height, the controls sit beside it.
            val gridSide = minOf(maxHeight - SCREEN_PADDING * 2, maxWidth * LANDSCAPE_GRID_SHARE, MAX_GRID_SIDE)
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = sideMargin, vertical = SCREEN_PADDING),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                board(gridSide)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = MAX_PANEL_WIDTH)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    status()
                    Spacer(Modifier.height(12.dp))
                    panel()
                }
            }
        } else {
            // Portrait: grid on top, controls below; the column scrolls if they don't fit.
            val gridSide = minOf(maxWidth - sideMargin * 2, maxHeight * PORTRAIT_GRID_SHARE, MAX_GRID_SIDE)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(SCREEN_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                status()
                Spacer(Modifier.height(8.dp))
                board(gridSide)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.widthIn(max = MAX_PANEL_WIDTH), horizontalAlignment = Alignment.CenterHorizontally) {
                    panel()
                }
            }
        }
      }

        if (state.isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.failedCheck?.let { failedCheck ->
            CheckFailedDialog(
                failedCheck = failedCheck,
                onKeepGoing = viewModel::dismissFailedCheck,
                onShowCount = viewModel::showWrongCount,
                onShowWhere = viewModel::showWrongCells
            )
        }

        if (state.showHowToPlay) {
            CrownsHowToPlayDialog(onClose = viewModel::closeHowToPlay)
        }

        if (confirmClear) {
            AlertDialog(
                onDismissRequest = { confirmClear = false },
                title = { Text(stringResource(R.string.crowns_clear_title)) },
                text = { Text(stringResource(R.string.crowns_clear_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmClear = false
                        viewModel.clearAll()
                    }) { Text(stringResource(R.string.action_clear)) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        }

        WinAnimationOverlay(
            visible = session.showWin,
            elapsedSeconds = session.elapsedSeconds,
            onPlayAgain = {
                viewModel.newPuzzle()
                soundManager.play(SoundEvent.TAP)
            },
            onHome = {
                viewModel.dismissWin()
                onBack()
            }
        )
    }
}

private val SCREEN_PADDING = 12.dp

/** A grid larger than this (tablets) only makes cells huge: cap it. */
private val MAX_GRID_SIDE = 560.dp
private val MAX_PANEL_WIDTH = 480.dp
private const val PORTRAIT_GRID_SHARE = 0.7f
private const val LANDSCAPE_GRID_SHARE = 0.6f

private class ControlActions(
    val onUndo: () -> Unit,
    val onToggleQuestionMode: () -> Unit,
    val onClearMarks: () -> Unit,
    val onClearAll: () -> Unit,
    val onCheck: () -> Unit
)

/** Hint message, the control row, Hardcore's Check button and the gesture reminder. */
@Composable
private fun ControlsPanel(
    state: CrownsUiState,
    isClassic: Boolean,
    onDismissHint: () -> Unit,
    actions: ControlActions
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        state.hint?.let { hint ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismissHint)
            ) {
                Text(
                    text = crownsHintMessage(hint.hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Controls(
            canUndo = state.canUndo,
            hasMarks = state.hasMarks,
            isQuestionMode = state.isQuestionMode,
            onUndo = actions.onUndo,
            onToggleQuestionMode = actions.onToggleQuestionMode,
            onClearMarks = actions.onClearMarks,
            onClearAll = actions.onClearAll
        )

        if (!isClassic) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = actions.onCheck,
                enabled = state.canCheck,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_check))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.crowns_gesture_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Space to leave on each side of the grid: the wider of the system gesture inset (zero with
 * 3-button navigation) and the screen's normal 12dp margin.
 */
@Composable
private fun gridSideMargin(): Dp {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val gestures = WindowInsets.systemGestures
    val gestureInset = with(density) {
        maxOf(gestures.getLeft(this, layoutDirection), gestures.getRight(this, layoutDirection)).toDp()
    }
    return maxOf(gestureInset, 12.dp)
}

/** Undo · ✕/? toggle · Clear marks · Clear all. Icon buttons keep the row within a 360dp phone. */
@Composable
private fun Controls(
    canUndo: Boolean,
    hasMarks: Boolean,
    isQuestionMode: Boolean,
    onUndo: () -> Unit,
    onToggleQuestionMode: () -> Unit,
    onClearMarks: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.action_undo))
        }
        FilledTonalButton(onClick = onToggleQuestionMode) {
            Text(stringResource(R.string.crowns_mark_with, if (isQuestionMode) "?" else "✕"))
        }
        FilledTonalIconButton(onClick = onClearMarks, enabled = hasMarks) {
            Icon(Icons.Default.CleaningServices, contentDescription = stringResource(R.string.crowns_clear_marks_description))
        }
        FilledTonalIconButton(onClick = onClearAll) {
            Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.crowns_clear_all_description))
        }
    }
}
