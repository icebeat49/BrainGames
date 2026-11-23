package com.payfacto.braingames.feature.wordsearch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfacto.braingames.R
import com.payfacto.braingames.core.sound.SoundEvent
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.core.ui.components.GameTopBar
import com.payfacto.braingames.core.ui.components.WinAnimationOverlay
import com.payfacto.braingames.feature.wordsearch.WordSearchViewModel

@Composable
fun WordSearchScreen(
    soundManager: SoundManager,
    onBack: () -> Unit,
    viewModel: WordSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()

    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { viewModel.onScreenHidden() }
    }

    LaunchedEffect(session.showWin) {
        if (session.showWin) soundManager.play(SoundEvent.WIN)
    }

    val found = state.board.words.count { it.found }
    val total = state.board.words.size
    val isComplete = state.board.isComplete()

    Scaffold(
        topBar = {
            GameTopBar(
                title = stringResource(R.string.home_play_wordsearch),
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
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Theme name
                if (state.board.theme.isNotEmpty()) {
                    Text(
                        text = state.board.theme,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                }

                Text(
                    text = stringResource(R.string.wordsearch_words_found, found, total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                WordSearchGrid(
                    board = state.board,
                    hintCell = state.hintCell,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    onSurface = MaterialTheme.colorScheme.onSurface,
                    revealHiddenWord = isComplete,
                    onSelectionComplete = { cells ->
                        val matched = viewModel.checkSelection(cells)
                        if (matched) soundManager.play(SoundEvent.CORRECT)
                        else soundManager.play(SoundEvent.ERROR)
                    }
                )

                Spacer(Modifier.height(10.dp))

                if (state.board.hiddenWord.isNotEmpty()) {
                    HiddenWordCard(
                        hiddenWord = state.board.hiddenWord,
                        hint = state.board.hint,
                        remainingLetters = state.board.remainingLetterCount(),
                        isRevealed = isComplete
                    )
                    Spacer(Modifier.height(10.dp))
                }

                WordList(words = state.board.words)
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

@Composable
private fun HiddenWordCard(
    hiddenWord: String,
    hint: String,
    remainingLetters: Int,
    isRevealed: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mot caché",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (!isRevealed) {
                    Text(
                        text = "$remainingLetters lettres restantes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                hiddenWord.forEach { ch ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                color = if (isRevealed)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isRevealed)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRevealed) ch.toString() else "_",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isRevealed) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRevealed)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Indice : $hint",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
            )
        }
    }
}
