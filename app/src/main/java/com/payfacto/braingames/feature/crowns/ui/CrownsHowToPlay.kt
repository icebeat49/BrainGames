package com.payfacto.braingames.feature.crowns.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.payfacto.braingames.R
import com.payfacto.braingames.feature.crowns.model.CellMark
import com.payfacto.braingames.feature.crowns.model.CrownsBoard
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle

/** One page: a title, an example board and the explanation. */
private data class HowToPlayPage(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val board: CrownsBoard,
    val highlights: CrownsGridHighlights = CrownsGridHighlights()
)

/**
 * Four short pages: the rules, marking, erasing, Classic vs Hardcore. Each has a small
 * example grid drawn by the game's own renderer. Closing it in any way counts as seen.
 */
@Composable
fun CrownsHowToPlayDialog(onClose: () -> Unit) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    val page = PAGES[pageIndex]
    val isLast = pageIndex == PAGES.lastIndex

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.how_to_play)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(page.title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().semantics { heading() }
                )
                CrownsBoardPicture(
                    puzzle = EXAMPLE,
                    board = page.board,
                    highlights = page.highlights,
                    description = stringResource(R.string.crowns_howto_example),
                    modifier = Modifier.widthIn(max = EXAMPLE_MAX_WIDTH).fillMaxWidth()
                )
                Text(text = stringResource(page.body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.how_to_play_page, pageIndex + 1, PAGES.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (isLast) onClose() else pageIndex++ }) {
                Text(stringResource(if (isLast) R.string.action_got_it else R.string.action_next))
            }
        },
        dismissButton = {
            if (pageIndex > 0) {
                TextButton(onClick = { pageIndex-- }) { Text(stringResource(R.string.action_previous)) }
            }
        }
    )
}

private val EXAMPLE_MAX_WIDTH = 220.dp

/** 5×5 example: one crown per row, column and region; crowns at columns 0, 2, 4, 1, 3. */
private val EXAMPLE = CrownsPuzzle(
    size = 5,
    regions = listOf("ABBCC", "BBBCC", "DDBCC", "DDEEC", "DEEEE")
        .joinToString("").map { it - 'A' }.toIntArray(),
    solution = intArrayOf(0, 2, 4, 1, 3)
)

private fun cell(row: Int, column: Int) = EXAMPLE.cellAt(row, column)

private val PAGES = listOf(
    HowToPlayPage(
        title = R.string.crowns_howto_goal_title,
        body = R.string.crowns_howto_goal_body,
        board = CrownsBoard.empty(5).withMarks(EXAMPLE.solutionCells, CellMark.CROWN)
    ),
    HowToPlayPage(
        title = R.string.crowns_howto_marks_title,
        body = R.string.crowns_howto_marks_body,
        board = CrownsBoard.empty(5)
            .withMark(cell(0, 0), CellMark.CROWN)
            .withMarks((1..4).map { cell(0, it) } + listOf(cell(1, 0), cell(1, 1)), CellMark.CROSS)
            .withMark(cell(2, 2), CellMark.QUESTION)
    ),
    HowToPlayPage(
        title = R.string.crowns_howto_erase_title,
        body = R.string.crowns_howto_erase_body,
        board = CrownsBoard.empty(5)
            .withMarks(listOf(cell(0, 0), cell(1, 2)), CellMark.CROWN)
            .withMarks(listOf(cell(4, 0), cell(4, 1)), CellMark.CROSS)
    ),
    HowToPlayPage(
        title = R.string.crowns_howto_modes_title,
        body = R.string.crowns_howto_modes_body,
        board = CrownsBoard.empty(5).withMarks(listOf(cell(0, 0), cell(1, 1)), CellMark.CROWN),
        highlights = CrownsGridHighlights(conflictCells = setOf(cell(0, 0), cell(1, 1)))
    )
)
