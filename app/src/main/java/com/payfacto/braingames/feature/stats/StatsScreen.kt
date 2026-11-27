package com.payfacto.braingames.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.stats.GameStats
import com.payfacto.braingames.core.ui.GameCatalog
import com.payfacto.braingames.core.ui.components.formatElapsed

/**
 * Played, won, best and average time per game, level and mode, with each game's totals.
 * Each game can be reset on its own, or all at once; both ask first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    var resetRequest by rememberSaveable { mutableStateOf<GameType?>(null) }
    var resetAllRequested by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = { resetAllRequested = true }) {
                        Text(stringResource(R.string.stats_reset_all))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sections, key = { it.game }) { section ->
                GameStatsCard(section, onReset = { resetRequest = section.game })
            }
        }
    }

    resetRequest?.let { game ->
        ResetDialog(
            title = stringResource(R.string.stats_reset_game_title, stringResource(GameCatalog.infoFor(game).titleRes)),
            onConfirm = {
                viewModel.reset(game)
                resetRequest = null
            },
            onDismiss = { resetRequest = null }
        )
    }
    if (resetAllRequested) {
        ResetDialog(
            title = stringResource(R.string.stats_reset_all_title),
            onConfirm = {
                viewModel.resetAll()
                resetAllRequested = false
            },
            onDismiss = { resetAllRequested = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameStatsCard(section: GameStatsSection, onReset: () -> Unit) {
    val title = stringResource(GameCatalog.infoFor(section.game).titleRes)
    var mode by rememberSaveable(section.game) { mutableStateOf(GameMode.CLASSIC) }
    val shown = section.modes[mode] ?: section.modes.values.first()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.stats_game_summary, section.total.played, section.total.won),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = stringResource(R.string.stats_reset_game, title)
                    )
                }
            }

            if (section.modes.size > 1) {
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    section.modes.keys.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == mode,
                            onClick = { mode = option },
                            shape = SegmentedButtonDefaults.itemShape(index, section.modes.size)
                        ) {
                            Text(stringResource(option.labelRes))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            StatsTable(shown)
        }
    }
}

@Composable
private fun StatsTable(stats: ModeStats) {
    Column {
        StatsRow(
            label = stringResource(R.string.stats_level),
            values = listOf(
                stringResource(R.string.stats_played),
                stringResource(R.string.stats_won),
                stringResource(R.string.stats_best),
                stringResource(R.string.stats_average)
            ),
            emphasized = true
        )
        HorizontalDivider()
        stats.byDifficulty.forEach { (difficulty, row) ->
            StatsRow(stringResource(difficulty.labelRes), row.cells())
        }
        HorizontalDivider()
        StatsRow(stringResource(R.string.stats_all_levels), stats.total.cells(), emphasized = true)
    }
}

@Composable
private fun StatsRow(label: String, values: List<String>, emphasized: Boolean = false) {
    val style = MaterialTheme.typography.bodySmall.let {
        if (emphasized) it.copy(fontWeight = FontWeight.SemiBold) else it
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = style, modifier = Modifier.weight(1.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        values.forEach { value ->
            Text(value, style = style, modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1)
        }
    }
}

/** Played, won, best, average; "–" for a time not set yet. */
private fun GameStats.cells(): List<String> = listOf(
    played.toString(),
    won.toString(),
    bestMs?.let { formatElapsed(it / 1_000) } ?: NO_TIME,
    averageMs?.let { formatElapsed(it / 1_000) } ?: NO_TIME
)

private const val NO_TIME = "–"

@Composable
private fun ResetDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(stringResource(R.string.stats_reset_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.stats_reset_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

private val GameMode.labelRes: Int
    get() = when (this) {
        GameMode.CLASSIC -> R.string.mode_classic
        GameMode.HARDCORE -> R.string.mode_hardcore
    }

private val Difficulty.labelRes: Int
    get() = when (this) {
        Difficulty.VERY_EASY -> R.string.difficulty_very_easy
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
        Difficulty.EXPERT -> R.string.difficulty_expert
    }
