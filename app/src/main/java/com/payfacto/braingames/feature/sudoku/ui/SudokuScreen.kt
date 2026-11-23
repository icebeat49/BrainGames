package com.payfacto.braingames.feature.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.sound.SoundEvent
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.core.ui.components.GameTopBar
import com.payfacto.braingames.core.ui.components.WinAnimationOverlay
import com.payfacto.braingames.feature.sudoku.SudokuViewModel

@Composable
fun SudokuScreen(
    difficulty: Difficulty,
    soundManager: SoundManager,
    onBack: () -> Unit,
    viewModel: SudokuViewModel = hiltViewModel()
) {
    LaunchedEffect(difficulty) {
        viewModel.init(difficulty)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.showWin) {
        if (state.showWin) soundManager.play(SoundEvent.WIN)
    }

    Scaffold(
        topBar = {
            GameTopBar(
                title = stringResource(R.string.home_play_sudoku),
                hintsRemaining = state.hintsRemaining,
                isMuted = state.isMuted,
                onBack = onBack,
                onHint = {
                    val used = viewModel.useHint()
                    if (used) soundManager.play(SoundEvent.CORRECT)
                },
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
                text = stringResource(R.string.hints_remaining, state.hintsRemaining),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            SudokuGrid(
                board = state.board,
                selectedCell = state.selectedCell,
                primaryColor = MaterialTheme.colorScheme.primary,
                onSurface = MaterialTheme.colorScheme.onSurface,
                onCellTap = { idx ->
                    if (!state.isGenerating) {
                        soundManager.play(SoundEvent.TAP)
                        viewModel.selectCell(idx)
                    }
                }
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
                onToggleNotesMode = { viewModel.toggleNotesMode() }
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
            visible = state.showWin,
            elapsedSeconds = state.elapsedSeconds,
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
