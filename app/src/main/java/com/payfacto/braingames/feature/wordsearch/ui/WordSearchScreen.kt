package com.payfacto.braingames.feature.wordsearch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.feature.wordsearch.SelectionResult
import com.payfacto.braingames.feature.wordsearch.WordSearchViewModel
import com.payfacto.braingames.feature.wordsearch.model.WordSearchBoard

@Composable
fun WordSearchScreen(
    soundManager: SoundManager,
    onBack: () -> Unit,
    viewModel: WordSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()

    KeepScreenOn()

    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { viewModel.onScreenHidden() }
    }

    WinSoundEffect(session.showWin, soundManager)

    val isClassic = state.mode == GameMode.CLASSIC
    val found = state.board.words.count { it.found }
    val total = state.board.words.size
    // Gold once there is something to read: the real secret when found, or in Classic the letters
    // left once every word is found. Hardcore leaves the reading to the player, as on paper.
    val leftoverCells = when {
        state.board.secretFound -> state.board.hiddenWordCells
        isClassic && state.board.allWordsFound() -> state.board.unmarkedCells()
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            GameTopBar(
                title = stringResource(R.string.home_play_wordsearch),
                hintsRemaining = session.hintsRemaining,
                isMuted = session.isMuted,
                onBack = onBack,
                onHint = if (isClassic) {
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
                    // Edge-to-edge: the window no longer shrinks for the keyboard, so make room
                    // for it here; the focused answer field then scrolls into view above it.
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(CONTENT_PADDING)
                    // Counts this padding toward the grid's side-swipe margin below, instead of
                    // adding the margin on top of it: the grid gets wider by twice the padding.
                    .consumeWindowInsets(PaddingValues(CONTENT_PADDING)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Counter in the middle, Undo at the end of the same line: close to the grid.
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(
                            if (isClassic) R.string.wordsearch_words_found else R.string.wordsearch_words_ticked,
                            found, total
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalIconButton(
                        onClick = { if (viewModel.undo()) soundManager.play(SoundEvent.TAP) },
                        enabled = state.canUndo && !state.board.secretFound,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.action_undo))
                    }
                }
                Spacer(Modifier.height(4.dp))

                WordSearchGrid(
                    board = state.board,
                    hintCell = state.hintCell,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    onSurface = MaterialTheme.colorScheme.onSurface,
                    leftoverCells = leftoverCells,
                    // Keeps the edge columns out of the side strips where a swipe means Back,
                    // so a word can be dragged from them (only the page padding with 3-button navigation).
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.systemGestures.only(WindowInsetsSides.Horizontal)
                    ),
                    onSelectionComplete = { cells ->
                        when (viewModel.checkSelection(cells)) {
                            SelectionResult.FOUND -> soundManager.play(SoundEvent.CORRECT)
                            SelectionResult.NOT_A_WORD -> soundManager.play(SoundEvent.ERROR)
                            // Same sound for every line, so it tells nothing about right or wrong.
                            SelectionResult.DRAWN, SelectionResult.ERASED -> soundManager.play(SoundEvent.TAP)
                            SelectionResult.IGNORED -> Unit
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))

                val secretCard: @Composable () -> Unit = {
                    if (state.board.hiddenWord.isNotEmpty()) {
                        SecretWordCard(
                            board = state.board,
                            onCheck = { guess ->
                                val right = viewModel.checkSecret(guess)
                                soundManager.play(if (right) SoundEvent.CORRECT else SoundEvent.ERROR)
                                right
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                val wordList: @Composable () -> Unit = {
                    WordList(
                        words = state.board.words,
                        onWordTapped = if (isClassic || state.board.secretFound) null else {
                            { word ->
                                soundManager.play(SoundEvent.TAP)
                                viewModel.onWordTapped(word)
                            }
                        }
                    )
                }
                // Hardcore ticks words off by hand, so the list sits right under the grid.
                if (isClassic) {
                    secretCard()
                    wordList()
                } else {
                    wordList()
                    secretCard()
                }
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
private fun SecretWordCard(
    board: WordSearchBoard,
    onCheck: (String) -> Boolean
) {
    var guess by rememberSaveable(board.hiddenWord) { mutableStateOf("") }
    var wrongGuess by rememberSaveable(board.hiddenWord) { mutableStateOf(false) }
    val check = {
        if (guess.isNotBlank()) wrongGuess = !onCheck(guess)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.wordsearch_secret_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = secretSizeLabel(board),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))

            if (board.secretFound) {
                Text(
                    text = stringResource(R.string.wordsearch_secret_found, board.hiddenWord),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                return@Column
            }

            Text(
                text = stringResource(R.string.wordsearch_secret_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = guess,
                    onValueChange = {
                        guess = it
                        wrongGuess = false
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.wordsearch_secret_input)) },
                    isError = wrongGuess,
                    // Part of the field, so it stays in view above the keyboard.
                    supportingText = if (wrongGuess) {
                        { Text(stringResource(R.string.wordsearch_secret_wrong)) }
                    } else null,
                    // Shown in capitals like the grid; the text itself is left as typed, since rewriting
                    // it under the keyboard's composition drops letters.
                    visualTransformation = UppercaseTransformation,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { check() })
                )
                // Top-aligned so the message under the field does not push it down; the offset
                // lines it up with the field's outline, which sits below the floating label.
                Button(onClick = check, enabled = guess.isNotBlank(), modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.wordsearch_secret_check))
                }
            }
        }
    }
}

/** "9 letters", or "11 letters · 2 words" for a phrase. */
@Composable
private fun secretSizeLabel(board: WordSearchBoard): String {
    val letters = pluralStringResource(
        R.plurals.wordsearch_secret_letters, board.secretLetterCount, board.secretLetterCount
    )
    if (board.secretWordCount <= 1) return letters
    val words = pluralStringResource(
        R.plurals.wordsearch_secret_words, board.secretWordCount, board.secretWordCount
    )
    return "$letters · $words"
}

private object UppercaseTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        // Char by char, so the length never changes (uppercase() turns "ß" into "SS").
        TransformedText(AnnotatedString(String(CharArray(text.length) { text[it].uppercaseChar() })), OffsetMapping.Identity)
}

private val CONTENT_PADDING = 8.dp
