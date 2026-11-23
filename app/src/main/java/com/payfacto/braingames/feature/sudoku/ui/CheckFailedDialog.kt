package com.payfacto.braingames.feature.sudoku.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.payfacto.braingames.R
import com.payfacto.braingames.feature.sudoku.model.FailedCheck

/**
 * Shown after a Hardcore check finds mistakes. The player chooses how much to learn:
 * nothing, how many cells are wrong, or which ones.
 */
@Composable
fun CheckFailedDialog(
    failedCheck: FailedCheck,
    onKeepGoing: () -> Unit,
    onShowCount: () -> Unit,
    onShowWhere: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepGoing,
        title = { Text(stringResource(R.string.sudoku_check_failed_title)) },
        text = {
            Text(
                if (failedCheck.isCountShown) {
                    pluralStringResource(
                        R.plurals.sudoku_check_wrong_count,
                        failedCheck.wrongCount,
                        failedCheck.wrongCount
                    )
                } else {
                    stringResource(R.string.sudoku_check_failed_message)
                }
            )
        },
        // Three actions with longer French labels: stacked vertically rather than in one row.
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (!failedCheck.isCountShown) {
                    TextButton(onClick = onShowCount) {
                        Text(stringResource(R.string.sudoku_check_show_count))
                    }
                }
                TextButton(onClick = onShowWhere) {
                    Text(stringResource(R.string.sudoku_check_show_where))
                }
                TextButton(onClick = onKeepGoing) {
                    Text(stringResource(R.string.sudoku_check_keep_going))
                }
            }
        }
    )
}
