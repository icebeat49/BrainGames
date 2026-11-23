package com.payfacto.braingames.feature.sudoku.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.ui.navigation.DifficultyPickerScreen
import com.payfacto.braingames.feature.sudoku.SudokuModeViewModel
import com.payfacto.braingames.feature.sudoku.model.SudokuMode

/** The generic difficulty picker plus Sudoku's Classic / Hardcore choice. */
@Composable
fun SudokuDifficultyPicker(
    onStart: (Difficulty, SudokuMode) -> Unit,
    onBack: () -> Unit,
    viewModel: SudokuModeViewModel = hiltViewModel()
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()

    DifficultyPickerScreen(
        gameType = GameType.SUDOKU,
        onDifficultySelected = { difficulty -> onStart(difficulty, mode) },
        onBack = onBack,
        extraOptions = { SudokuModeSelector(selected = mode, onSelect = viewModel::select) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SudokuModeSelector(selected: SudokuMode, onSelect: (SudokuMode) -> Unit) {
    val modes = SudokuMode.entries
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(selected.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private val SudokuMode.labelRes: Int
    get() = when (this) {
        SudokuMode.CLASSIC  -> R.string.sudoku_mode_classic
        SudokuMode.HARDCORE -> R.string.sudoku_mode_hardcore
    }

private val SudokuMode.descriptionRes: Int
    get() = when (this) {
        SudokuMode.CLASSIC  -> R.string.sudoku_mode_classic_description
        SudokuMode.HARDCORE -> R.string.sudoku_mode_hardcore_description
    }
