package com.payfacto.braingames.feature.animals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.CheckResult
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.sound.SoundEvent
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.core.ui.components.CheckFailedDialog
import com.payfacto.braingames.core.ui.components.GameTopBar
import com.payfacto.braingames.core.ui.components.WinAnimationOverlay
import com.payfacto.braingames.feature.animals.AnimalsViewModel

@Composable
fun AnimalsScreen(
    soundManager: SoundManager,
    onBack: () -> Unit,
    viewModel: AnimalsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isClassic = state.mode == GameMode.CLASSIC
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { viewModel.onScreenHidden() }
    }

    LaunchedEffect(session.showWin) {
        if (session.showWin) soundManager.play(SoundEvent.WIN)
    }

    Scaffold(
        topBar = {
            GameTopBar(
                title = stringResource(R.string.home_play_animals),
                hintsRemaining = session.hintsRemaining,
                isMuted = session.isMuted,
                onBack = onBack,
                onHint = if (isClassic) {
                    { if (viewModel.useHint()) soundManager.play(SoundEvent.CORRECT) }
                } else null,
                onNewPuzzle = {
                    soundManager.play(SoundEvent.TAP)
                    viewModel.newPuzzle()
                },
                onToggleMute = viewModel::toggleMute
            )
        }
    ) { padding ->
      BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        // Square grid that also fits in landscape; the column scrolls for the controls.
        val gridSide = min(maxWidth - 24.dp, maxHeight * 0.7f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isClassic) {
                    stringResource(R.string.hints_remaining, session.hintsRemaining)
                } else {
                    stringResource(R.string.mode_hardcore_label)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            val puzzle = state.puzzle
            if (puzzle != null && puzzle.size == state.board.size) {
                AnimalsGrid(
                    puzzle = puzzle,
                    board = state.board,
                    highlights = AnimalsGridHighlights(
                        conflictCells = state.conflictCells,
                        errorCells = state.errorCells,
                        hintCells = state.hint?.cells.orEmpty(),
                        hintAreaCells = state.hint?.areaCells.orEmpty()
                    ),
                    isQuestionMode = state.isQuestionMode,
                    input = AnimalsGridInput(
                        onTap = viewModel::onTap,
                        onDragStart = viewModel::onDragStart,
                        onDragMove = viewModel::onDragMove,
                        onDragEnd = viewModel::onDragEnd,
                        onSetMark = viewModel::setMark
                    ),
                    modifier = Modifier.size(gridSide)
                )
            }

            state.hint?.let { hint ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = viewModel::dismissHint)
                ) {
                    Text(
                        text = animalsHintMessage(hint.hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Controls(
                canUndo = state.canUndo,
                isQuestionMode = state.isQuestionMode,
                onUndo = {
                    viewModel.undo()
                    soundManager.play(SoundEvent.TAP)
                },
                onToggleQuestionMode = viewModel::toggleQuestionMode,
                onClearAll = { confirmClear = true }
            )

            if (!isClassic) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (viewModel.checkSolution() is CheckResult.Incorrect) soundManager.play(SoundEvent.ERROR)
                    },
                    enabled = state.canCheck,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_check))
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.animals_gesture_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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

        if (confirmClear) {
            AlertDialog(
                onDismissRequest = { confirmClear = false },
                title = { Text(stringResource(R.string.animals_clear_title)) },
                text = { Text(stringResource(R.string.animals_clear_message)) },
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

@Composable
private fun Controls(
    canUndo: Boolean,
    isQuestionMode: Boolean,
    onUndo: () -> Unit,
    onToggleQuestionMode: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.action_undo))
        }
        Spacer(Modifier.width(12.dp))
        FilledTonalButton(onClick = onToggleQuestionMode) {
            Text(stringResource(R.string.animals_mark_with, if (isQuestionMode) "?" else "✕"))
        }
        Spacer(Modifier.width(12.dp))
        FilledTonalButton(onClick = onClearAll) {
            Text(stringResource(R.string.action_clear_all))
        }
    }
}
