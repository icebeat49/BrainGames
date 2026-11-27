package com.payfacto.braingames.core.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.save.GameSave
import com.payfacto.braingames.core.ui.GameCatalog
import com.payfacto.braingames.core.ui.GameInfo
import com.payfacto.braingames.core.ui.components.formatElapsed

/**
 * Difficulty choice for any game. Games whose catalog entry offers modes also get a
 * Classic / Hardcore selector (last choice remembered per game), and games with a
 * meaningful grid size show it next to each level.
 *
 * When a game is in progress, Continue comes first, and starting a new game asks before
 * replacing it. [onStart] gets `resume = true` for Continue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultyPickerScreen(
    gameType: GameType,
    onStart: (difficulty: Difficulty, mode: GameMode, resume: Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: DifficultyPickerViewModel = hiltViewModel()
) {
    val info = GameCatalog.infoFor(gameType)
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    /** The level waiting for "replace the game in progress?" to be confirmed. */
    var pendingNewGame by rememberSaveable { mutableStateOf<Difficulty?>(null) }
    val chosenMode = if (info.supportsModes) mode else GameMode.CLASSIC

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(info.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        // Scrolls when Continue, the mode choice and six levels (Crowns) don't fit, and stays
        // centred when they do.
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Home shows only icons and names, so the game is described here.
            Text(
                text = stringResource(info.descriptionRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            saved?.let { save ->
                ContinueButton(info, save, onClick = { onStart(save.difficulty, save.mode, true) })
                Spacer(Modifier.height(32.dp))
            }
            Text(
                text = stringResource(if (saved == null) R.string.difficulty_select else R.string.picker_new_game),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(32.dp))
            if (info.supportsModes) {
                ModeSelector(info, selected = mode, onSelect = viewModel::select)
                Spacer(Modifier.height(24.dp))
            }
            gameType.levels.forEach { difficulty ->
                val label = difficultyLabel(difficulty, info.gridSize?.invoke(difficulty))
                val onClick = {
                    if (saved == null) onStart(difficulty, chosenMode, false) else pendingNewGame = difficulty
                }
                val modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                // With a game in progress, Continue is the main action and new games step back.
                if (saved == null) {
                    Button(onClick = onClick, modifier = modifier) { Text(label) }
                } else {
                    OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
                }
            }
        }
        }
    }

    pendingNewGame?.let { difficulty ->
        ReplaceGameDialog(
            onConfirm = {
                pendingNewGame = null
                onStart(difficulty, chosenMode, false)
            },
            onDismiss = { pendingNewGame = null }
        )
    }
}

/** "Continue" with what the saved game is: level, mode when the game has modes, and time played. */
@Composable
private fun ContinueButton(info: GameInfo, save: GameSave, onClick: () -> Unit) {
    val details = listOfNotNull(
        difficultyLabel(save.difficulty, info.gridSize?.invoke(save.difficulty)),
        if (info.supportsModes) stringResource(save.mode.labelRes) else null,
        formatElapsed(save.elapsedSeconds)
    ).joinToString(" · ")
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(stringResource(R.string.picker_continue), style = MaterialTheme.typography.titleMedium)
            Text(details, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReplaceGameDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.picker_replace_title)) },
        text = { Text(stringResource(R.string.picker_replace_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.picker_replace_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(info: GameInfo, selected: GameMode, onSelect: (GameMode) -> Unit) {
    val modes = GameMode.entries
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                ) {
                    Text(stringResource(mode.labelRes))
                }
            }
        }
        info.modeDescriptions?.get(selected)?.let { descriptionRes ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val GameMode.labelRes: Int
    get() = when (this) {
        GameMode.CLASSIC  -> R.string.mode_classic
        GameMode.HARDCORE -> R.string.mode_hardcore
    }

@Composable
private fun difficultyLabel(difficulty: Difficulty, gridSize: Int?): String {
    val name = stringResource(
        when (difficulty) {
            Difficulty.VERY_EASY -> R.string.difficulty_very_easy
            Difficulty.EASY      -> R.string.difficulty_easy
            Difficulty.MEDIUM    -> R.string.difficulty_medium
            Difficulty.HARD      -> R.string.difficulty_hard
            Difficulty.EXPERT    -> R.string.difficulty_expert
            Difficulty.MASTER    -> R.string.difficulty_master
        }
    )
    return if (gridSize == null) name else stringResource(R.string.difficulty_with_size, name, gridSize)
}
