package com.payfacto.braingames.feature.kakuro.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.payfacto.braingames.core.sound.SoundEvent
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.core.ui.components.GameTopBar
import com.payfacto.braingames.core.ui.components.KeepScreenOn
import com.payfacto.braingames.core.ui.components.WinAnimationOverlay
import com.payfacto.braingames.core.ui.components.WinSoundEffect
import com.payfacto.braingames.feature.kakuro.KakuroViewModel
import com.payfacto.braingames.feature.sudoku.ui.NumberPad

@Composable
fun KakuroScreen(
    soundManager: SoundManager,
    onBack: () -> Unit,
    viewModel: KakuroViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()

    KeepScreenOn()

    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { viewModel.onScreenHidden() }
    }

    WinSoundEffect(session.showWin, soundManager)

    Scaffold(
        topBar = {
            GameTopBar(
                title = stringResource(R.string.home_play_kakuro),
                hintsRemaining = session.hintsRemaining,
                isMuted = session.isMuted,
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
                text = stringResource(R.string.hints_remaining, session.hintsRemaining),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            KakuroGrid(
                board = state.board,
                selectedCell = state.selectedCell,
                onCellTap = { r, c ->
                    soundManager.play(SoundEvent.TAP)
                    viewModel.selectCell(r, c)
                }
            )
            Spacer(Modifier.height(16.dp))
            NumberPad(
                onNumber = { n ->
                    viewModel.enterNumber(n)
                    // The board after this move: `state` is still the one from before it.
                    if (viewModel.state.value.board.hasConflicts()) soundManager.play(SoundEvent.ERROR)
                    else soundManager.play(SoundEvent.TAP)
                },
                onClear = {
                    viewModel.clearCell()
                    soundManager.play(SoundEvent.TAP)
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
