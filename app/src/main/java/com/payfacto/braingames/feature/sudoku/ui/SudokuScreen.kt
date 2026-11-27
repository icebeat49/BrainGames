package com.payfacto.braingames.feature.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.CheckResult
import com.payfacto.braingames.core.sound.SoundEvent
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.core.ui.components.CheckFailedDialog
import com.payfacto.braingames.core.ui.components.GameTopBar
import com.payfacto.braingames.core.ui.components.KeepScreenOn
import com.payfacto.braingames.core.ui.components.WinAnimationOverlay
import com.payfacto.braingames.core.ui.components.WinSoundEffect
import com.payfacto.braingames.feature.sudoku.SudokuViewModel
import com.payfacto.braingames.feature.sudoku.model.SudokuAssists

@Composable
fun SudokuScreen(
    soundManager: SoundManager,
    onBack: () -> Unit,
    viewModel: SudokuViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val assists = SudokuAssists.forMode(state.mode)

    KeepScreenOn()

    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { viewModel.onScreenHidden() }
    }

    WinSoundEffect(session.showWin, soundManager)

    Scaffold(
        topBar = {
            GameTopBar(
                title = stringResource(R.string.home_play_sudoku),
                hintsRemaining = session.hintsRemaining,
                isMuted = session.isMuted,
                onBack = onBack,
                onHint = if (assists.hintsAllowed) {
                    {
                        val used = viewModel.useHint()
                        if (used) soundManager.play(SoundEvent.CORRECT)
                    }
                } else null,
                onNewPuzzle = {
                    soundManager.play(SoundEvent.TAP)
                    viewModel.newPuzzle()
                },
                onToggleMute = { viewModel.toggleMute() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (assists.hintsAllowed) {
                    stringResource(R.string.hints_remaining, session.hintsRemaining)
                } else {
                    stringResource(R.string.mode_hardcore_label)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            SudokuGrid(
                board = state.board,
                selectedCell = state.selectedCell,
                assists = assists,
                primaryColor = MaterialTheme.colorScheme.primary,
                onSurface = MaterialTheme.colorScheme.onSurface,
                onCellTap = { idx ->
                    if (!state.isGenerating) {
                        soundManager.play(SoundEvent.TAP)
                        viewModel.selectCell(idx)
                    }
                },
                errorCells = state.errorCells,
                errorColor = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            NumberPad(
                onNumber = { n ->
                    viewModel.enterNumber(n)
                    soundManager.play(SoundEvent.TAP)
                },
                onClear = {
                    viewModel.clearCell()
                    soundManager.play(SoundEvent.TAP)
                },
                isNotesMode = state.isNotesMode,
                onToggleNotesMode = { viewModel.toggleNotesMode() },
                onUndo = {
                    viewModel.undo()
                    soundManager.play(SoundEvent.TAP)
                },
                canUndo = state.canUndo
            )
            if (!assists.autoDetectWin) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val result = viewModel.checkSolution()
                        if (result is CheckResult.Incorrect) soundManager.play(SoundEvent.ERROR)
                    },
                    enabled = state.canCheck,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_check))
                }
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
