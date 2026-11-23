package com.payfacto.braingames.core.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.payfacto.braingames.core.ui.GameCatalog
import com.payfacto.braingames.core.ui.GameInfo

/**
 * Difficulty choice for any game. Games whose catalog entry offers modes also get a
 * Classic / Hardcore selector (last choice remembered per game), and games with a
 * meaningful grid size show it next to each level.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultyPickerScreen(
    gameType: GameType,
    onStart: (Difficulty, GameMode) -> Unit,
    onBack: () -> Unit,
    viewModel: DifficultyPickerViewModel = hiltViewModel()
) {
    val info = GameCatalog.infoFor(gameType)
    val mode by viewModel.mode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(info.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.difficulty_select),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(32.dp))
            if (info.supportsModes) {
                ModeSelector(info, selected = mode, onSelect = viewModel::select)
                Spacer(Modifier.height(24.dp))
            }
            Difficulty.entries.forEach { difficulty ->
                Button(
                    onClick = { onStart(difficulty, if (info.supportsModes) mode else GameMode.CLASSIC) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(difficultyLabel(difficulty, info.gridSize?.invoke(difficulty)))
                }
            }
        }
    }
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
        }
    )
    return if (gridSize == null) name else stringResource(R.string.difficulty_with_size, name, gridSize)
}
